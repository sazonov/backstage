/*
 *
 *  Copyright 2019-2023 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.proit.app.dict.service.query;

import com.google.common.collect.Sets;
import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.exception.dict.field.FieldNotFoundException;
import com.proit.app.dict.service.query.ast.*;
import com.proit.app.exception.AppException;
import com.proit.app.model.other.exception.ApiStatusCodeImpl;
import com.proit.app.dict.model.postgres.backend.PostgresWord;
import com.proit.app.dict.model.postgres.query.PostgresQueryContext;
import com.proit.app.dict.model.postgres.query.PostgresQueryField;
import com.proit.app.dict.service.backend.postgres.PostgresReservedKeyword;
import com.proit.app.utils.JsonUtils;
import com.proit.app.dict.constant.ServiceFieldConstants;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PostgresTranslator implements Translator<PostgresQueryContext>, Visitor<TranslationResult<?>>
{
	private final PostgresReservedKeyword reservedKeyword;

	@Override
	public PostgresQueryContext process(Dict dict, QueryExpression queryExpression)
	{
		return queryExpression.process(this, new TranslationContext(dict)).value(PostgresQueryContext.class);
	}

	@Override
	public TranslationResult<Object> visit(Constant constant, TranslationContext context)
	{
		return new TranslationResult<>(constant.getValue());
	}

	@Override
	public TranslationResult<PostgresQueryField> visit(Field field, TranslationContext context)
	{
		context.dict()
				.getFields()
				.stream()
				.filter(it -> it.getId().equalsIgnoreCase(field.name) || Set.of(ServiceFieldConstants.ID, ServiceFieldConstants._ID).contains(field.name))
				.findFirst()
				.orElseThrow(() -> new FieldNotFoundException(context.dict().getId(), field.name));

		return new TranslationResult<>(new PostgresQueryField(field.dictId == null ? context.dict().getId() : field.dictId, field.name, reservedKeyword));
	}

	@Override
	public TranslationResult<PostgresQueryContext> visit(LikeQueryExpression expression, TranslationContext context)
	{
		var field = expression.field.process(this, context).value(PostgresQueryField.class);
		var template = expression.template.process(this, context).value(String.class);

		var queryContext = new PostgresQueryContext("%s ~ '.*%s.*'".formatted(field.getJoint(), template), participantDictIds(field));

		return new TranslationResult<>(queryContext);
	}

	@Override
	public TranslationResult<PostgresQueryContext> visit(IlikeQueryExpression expression, TranslationContext context)
	{
		var field = expression.field.process(this, context).value(PostgresQueryField.class);
		var template = expression.template.process(this, context).value(String.class);

		var queryContext = new PostgresQueryContext("%s ~* '.*%s.*'".formatted(field.getJoint(), template), participantDictIds(field));

		return new TranslationResult<>(queryContext);
	}

	@Override
	public TranslationResult<PostgresQueryContext> visit(LogicQueryExpression expression, TranslationContext context)
	{
		var left = expression.left.process(this, context).value(PostgresQueryContext.class);
		var right = expression.right.process(this, context).value(PostgresQueryContext.class);

		PostgresQueryContext query = null;

		var leftExpression = left.getSqlExpression();
		var rightExpression = right.getSqlExpression();

		var participantDictIds = Sets.union(left.getParticipantDictIds(), right.getParticipantDictIds());

		switch (expression.type)
		{
			case AND -> query = new PostgresQueryContext("(%s and %s)".formatted(leftExpression, rightExpression), participantDictIds);
			case OR -> query = new PostgresQueryContext("(%s or %s)".formatted(leftExpression, rightExpression), participantDictIds);
			case NOT -> query = new PostgresQueryContext("not (%s)".formatted(leftExpression), participantDictIds);

			default -> throw new AppException(ApiStatusCodeImpl.ILLEGAL_INPUT, "Не поддерживаемый оператор '%s'".formatted(expression.type));
		}

		return new TranslationResult<>(query);
	}

	@Override
	public TranslationResult<PostgresQueryContext> visit(Predicate predicate, TranslationContext context)
	{
		var left = predicate.left.process(this, context).value(PostgresQueryField.class);
		var right = predicate.right.process(this, context);

		var sqlExpression = left.getJoint();

		switch (predicate.type)
		{
			case EQ -> sqlExpression += " %s %s".formatted(predicateRightValue(right) == null ? "is" : "=", predicateRightValue(right));
			case NEQ -> sqlExpression += " %s %s".formatted(predicateRightValue(right) == null ? "is not" : "!=", predicateRightValue(right));
			case LS -> sqlExpression += " < %s".formatted(predicateRightValue(right));
			case GT -> sqlExpression += " > %s".formatted(predicateRightValue(right));
			case LEQ -> sqlExpression += " <= %s".formatted(predicateRightValue(right));
			case GEQ -> sqlExpression += " >= %s".formatted(predicateRightValue(right));

			default -> throw new AppException(ApiStatusCodeImpl.ILLEGAL_INPUT, "Не поддерживаемый оператор '%s'".formatted(predicate.type));
		}

		return new TranslationResult<>(new PostgresQueryContext(sqlExpression, participantDictIds(left)));
	}

	@Override
	public TranslationResult<PostgresQueryContext> visit(InQueryExpression expression, TranslationContext context)
	{
		var left = expression.field.process(this, context).value(PostgresQueryField.class);
		var variants = expression.variants.stream()
				.map(it -> it.process(this, context))
				.map(TranslationResult::value)
				.map(it -> "'" + it + "'")
				.collect(Collectors.joining(", "));

		var queryContext = new PostgresQueryContext("%s in (%s)".formatted(left.getJoint(), variants), participantDictIds(left));

		return new TranslationResult<>(queryContext);
	}

	@Override
	public TranslationResult<PostgresQueryContext> visit(AllOrAnyQueryExpression expression, TranslationContext context)
	{
		var left = expression.field.process(this, context).value(PostgresQueryField.class);
		var constants = expression.constants.stream()
				.map(it -> it.process(this, context))
				.map(TranslationResult::value)
				.map(JsonUtils::toJson)
				.collect(Collectors.joining(", ", "{", "}"));

		var sqlExpression = left.getJoint();

		switch (expression.type)
		{
			case ALL -> sqlExpression += " %s '%s'".formatted("@>", constants);
			case ANY -> sqlExpression += " %s '%s'".formatted("&&", constants);

			default -> throw new AppException(ApiStatusCodeImpl.ILLEGAL_INPUT, "Не поддерживаемый оператор '%s'".formatted(expression.type));
		}

		return new TranslationResult<>(new PostgresQueryContext(sqlExpression, participantDictIds(left)));
	}

	@Override
	public TranslationResult<PostgresQueryContext> visit(Empty empty, TranslationContext context)
	{
		return new TranslationResult<>(new PostgresQueryContext());
	}

	private Object predicateRightValue(TranslationResult<?> right)
	{
		return right.value() == null ? null : "'%s'".formatted(right.value());
	}

	private Set<PostgresWord> participantDictIds(PostgresQueryField field)
	{
		return field.getDictId() != null ? Set.of(field.getDictId()) : Collections.emptySet();
	}
}
