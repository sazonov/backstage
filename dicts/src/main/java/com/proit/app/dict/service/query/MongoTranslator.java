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

package com.proit.app.dict.service.query;

import com.google.common.collect.Sets;
import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.exception.dict.field.FieldNotFoundException;
import com.proit.app.dict.model.mongo.query.MongoQueryContext;
import com.proit.app.dict.model.mongo.query.MongoQueryField;
import com.proit.app.dict.service.query.ast.*;
import com.proit.app.exception.AppException;
import com.proit.app.model.other.exception.ApiStatusCodeImpl;
import com.proit.app.dict.constant.ServiceFieldConstants;
import lombok.RequiredArgsConstructor;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Collections;
import java.util.Set;

@RequiredArgsConstructor
public class MongoTranslator implements Translator<MongoQueryContext>, Visitor<TranslationResult<?>>
{
	@Override
	public MongoQueryContext process(Dict dict, QueryExpression queryExpression)
	{
		return queryExpression.process(this, new TranslationContext(dict)).value(MongoQueryContext.class);
	}

	@Override
	public TranslationResult<Object> visit(Constant constant, TranslationContext context)
	{
		//TODO: убрать после реализации валидации/маппинга QueryExpression для адаптеров
		if (Constant.Type.DECIMAL.equals(constant.type))
		{
			return new TranslationResult<>(Decimal128.parse((String) constant.getValue()));
		}

		return new TranslationResult<>(constant.getValue());
	}

	@Override
	public TranslationResult<MongoQueryField> visit(Field field, TranslationContext context)
	{
		context.dict()
				.getFields()
				.stream()
				.filter(it -> it.getId().equalsIgnoreCase(field.name) || Set.of(ServiceFieldConstants.ID, ServiceFieldConstants._ID).contains(field.name))
				.findFirst()
				.orElseThrow(() -> new FieldNotFoundException(context.dict().getId(), field.name));

		var fieldName = field.name.equals(ServiceFieldConstants.ID) ? ServiceFieldConstants._ID : field.name;

		return new TranslationResult<>(new MongoQueryField(field.dictId == null ? context.dict().getId() : field.dictId, fieldName));
	}

	@Override
	public TranslationResult<MongoQueryContext> visit(LikeQueryExpression expression, TranslationContext context)
	{
		var field = expression.field.process(this, context).value(MongoQueryField.class);
		var template = expression.template.process(this, context);

		var criteria = Criteria.where(field.getFieldId())
				.regex(template.value(String.class));

		return new TranslationResult<>(new MongoQueryContext(criteria, participantDictIds(field)));
	}

	@Override
	public TranslationResult<MongoQueryContext> visit(IlikeQueryExpression expression, TranslationContext context)
	{
		var field = expression.field.process(this, context).value(MongoQueryField.class);
		var template = expression.template.process(this, context);

		var criteria = Criteria.where(field.getFieldId())
				.regex(template.value(String.class), "i");

		return new TranslationResult<>(new MongoQueryContext(criteria, participantDictIds(field)));
	}

	@Override
	public TranslationResult<MongoQueryContext> visit(LogicQueryExpression expression, TranslationContext context)
	{
		var left = expression.left.process(this, context).value(MongoQueryContext.class);
		var right = expression.right.process(this, context).value(MongoQueryContext.class);

		MongoQueryContext queryContext;

		var participantDictIds = Sets.union(left.getParticipantDictIds(), right.getParticipantDictIds());

		switch (expression.type)
		{
			case OR -> queryContext = new MongoQueryContext(new Criteria().orOperator(left.getCriteria(), right.getCriteria()), participantDictIds);
			case AND -> queryContext = new MongoQueryContext(new Criteria().andOperator(left.getCriteria(), right.getCriteria()), participantDictIds);
			//fixme: исправить
			case NOT -> throw new UnsupportedOperationException("Оператор '%s' не поддерживается в MongoDictDataBackend.".formatted(expression.type));

			default -> throw new AppException(ApiStatusCodeImpl.ILLEGAL_INPUT, "Не поддерживаемый оператор '%s'".formatted(expression.type));
		}

		return new TranslationResult<>(queryContext);
	}

	@Override
	public TranslationResult<MongoQueryContext> visit(Predicate predicate, TranslationContext context)
	{
		var left = predicate.left.process(this, context).value(MongoQueryField.class);
		var right = predicate.right.process(this, context);

		var criteria = Criteria.where(left.getFieldId());

		switch (predicate.type)
		{
			case EQ -> criteria.is(right.value());
			case NEQ -> criteria.ne(right.value());
			case LS -> criteria.lt(right.value());
			case GT -> criteria.gt(right.value());
			case LEQ -> criteria.lte(right.value());
			case GEQ -> criteria.gte(right.value());

			default -> throw new AppException(ApiStatusCodeImpl.ILLEGAL_INPUT, "Не поддерживаемый оператор '%s'".formatted(predicate.type));
		}

		return new TranslationResult<>(new MongoQueryContext(criteria, participantDictIds(left)));
	}

	@Override
	public TranslationResult<MongoQueryContext> visit(InQueryExpression expression, TranslationContext context)
	{
		var left = expression.field.process(this, context).value(MongoQueryField.class);
		var variants = expression.variants.stream()
				.map(it -> it.process(this, context))
				.map(TranslationResult::value)
				.toList();

		var criteria = Criteria.where(left.getFieldId())
				.in(variants);

		return new TranslationResult<>(new MongoQueryContext(criteria, participantDictIds(left)));
	}

	@Override
	public TranslationResult<MongoQueryContext> visit(AllOrAnyQueryExpression expression, TranslationContext context)
	{
		var left = expression.field.process(this, context).value(MongoQueryField.class);
		var constants = expression.constants.stream()
				.map(it -> it.process(this, context))
				.map(TranslationResult::value)
				.toList();

		var criteria = Criteria.where(left.getFieldId());

		switch (expression.type)
		{
			case ALL -> criteria.all(constants);
			case ANY -> criteria.in(constants);

			default -> throw new AppException(ApiStatusCodeImpl.ILLEGAL_INPUT, "Не поддерживаемый оператор '%s'".formatted(expression.type));
		}

		return new TranslationResult<>(new MongoQueryContext(criteria, participantDictIds(left)));
	}

	@Override
	public TranslationResult<MongoQueryContext> visit(Empty empty, TranslationContext context)
	{
		return new TranslationResult<>(new MongoQueryContext());
	}

	private Set<String> participantDictIds(MongoQueryField field)
	{
		return field.getDictId() != null ? Set.of(field.getDictId()) : Collections.emptySet();
	}
}
