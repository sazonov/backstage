/*
 *    Copyright 2019-2023 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.proit.app.service.query;

import com.proit.app.domain.Dict;
import com.proit.app.exception.dictionary.field.FieldNotFoundException;
import com.proit.app.service.query.ast.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Set;
import java.util.stream.Collectors;

import static com.proit.app.constant.ServiceFieldConstants.ID;
import static com.proit.app.constant.ServiceFieldConstants._ID;

@RequiredArgsConstructor
public class MongoTranslator implements Translator<Criteria>, Visitor<TranslationResult<?>>
{
	public Criteria process(Dict dict, QueryExpression queryExpression)
	{
		return queryExpression.process(this, new TranslationContext(dict)).value(Criteria.class);
	}

	public TranslationResult<Object> visit(Constant constant, TranslationContext context)
	{
		return new TranslationResult<>(constant.getValue());
	}

	public TranslationResult<String> visit(Field field, TranslationContext context)
	{
		context.dict()
				.getFields()
				.stream()
				.filter(it -> it.getId().equalsIgnoreCase(field.name) || Set.of(ID, _ID).contains(field.name))
				.findFirst()
				.orElseThrow(() -> new FieldNotFoundException(context.dict().getId(), field.name));

		return new TranslationResult<>(field.name.equals(ID) ? _ID : field.name);
	}

	public TranslationResult<Criteria> visit(LikeQueryExpression expression, TranslationContext context)
	{
		var field = expression.field.process(this, context);
		var template = expression.template.process(this, context);

		return new TranslationResult<>(Criteria.where(field.value(String.class)).regex(template.value(String.class)));
	}

	public TranslationResult<Criteria> visit(IlikeQueryExpression expression, TranslationContext context)
	{
		var field = expression.field.process(this, context);
		var template = expression.template.process(this, context);

		return new TranslationResult<>(Criteria.where(field.value(String.class)).regex(template.value(String.class), "i"));
	}

	public TranslationResult<Criteria> visit(LogicQueryExpression expression, TranslationContext context)
	{
		var left = expression.left.process(this, context);
		var right = expression.right.process(this, context);

		Criteria criteria = null;

		switch (expression.type)
		{
			case OR -> criteria = new Criteria().orOperator(left.value(Criteria.class), right.value(Criteria.class));
			case AND -> criteria = new Criteria().andOperator(left.value(Criteria.class), right.value(Criteria.class));
			case NOT -> criteria = new Criteria().not().andOperator(left.value(Criteria.class));
		}

		return new TranslationResult<>(criteria);
	}

	public TranslationResult<Criteria> visit(Predicate predicate, TranslationContext context)
	{
		var left = predicate.left.process(this, context);
		var right = predicate.right.process(this, context);

		var criteria = Criteria.where(left.value(String.class));

		switch (predicate.type)
		{
			case EQ -> criteria.is(right.value());
			case NEQ -> criteria.ne(right.value());
			case LS -> criteria.lt(right.value());
			case GT -> criteria.gt(right.value());
			case LEQ -> criteria.lte(right.value());
			case GEQ -> criteria.gte(right.value());
		}

		return new TranslationResult<>(criteria);
	}

	public TranslationResult<Criteria> visit(InQueryExpression expression, TranslationContext context)
	{
		var left = expression.field.process(this, context);
		var variants = expression.variants.stream().map(it -> it.process(this, context)).collect(Collectors.toList());

		return new TranslationResult<>(Criteria.where(left.value(String.class)).in(variants.stream().map(TranslationResult::value).collect(Collectors.toList())));
	}

	public TranslationResult<Criteria> visit(Empty empty, TranslationContext context)
	{
		return new TranslationResult<>(new Criteria());
	}
}
