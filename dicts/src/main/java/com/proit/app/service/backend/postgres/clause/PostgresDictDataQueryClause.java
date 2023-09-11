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

package com.proit.app.service.backend.postgres.clause;

import com.proit.app.domain.DictField;
import com.proit.app.model.postgres.backend.PostgresDictFieldName;
import com.proit.app.model.postgres.backend.PostgresOrder;
import com.proit.app.model.postgres.backend.PostgresPageable;
import com.proit.app.model.postgres.backend.PostgresWord;
import com.proit.app.model.postgres.query.PostgresQueryContext;
import com.proit.app.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostgresDictDataQueryClause
{
	private static final String SELECT_CLAUSE = "%s.%s as %s__%s";
	private static final String JOIN_CLAUSE = "join %s on %s.%s = %s.%s";

	private final DictService dictService;

	public void addSelectClauses(HashSet<String> selectClauses, List<PostgresDictFieldName> requiredFields,
	                             PostgresQueryContext queryContext, PostgresPageable postgresPageable, String dictId)
	{
		addSelectIdClause(selectClauses, requiredFields, queryContext, postgresPageable, dictId);

		var isAllFields = requiredFields.stream()
				.map(PostgresDictFieldName::getWordFieldId)
				.map(PostgresWord::getOriginalWord)
				.allMatch("*"::equals);

		if (isAllFields)
		{
			requiredFields.stream()
					.map(field -> dictService.getById(field.getWordDictId().getOriginalWord())
							.getFields()
							.stream()
							.map(DictField::getId)
							.map(it -> selectClause(field, it))
							.collect(Collectors.toCollection(LinkedHashSet::new)))
					.forEach(selectClauses::addAll);

			return;
		}

		requiredFields.stream()
				.filter(it -> "*".equals(it.getWordFieldId().getOriginalWord()))
				.map(field -> dictService.getById(field.getWordDictId().getOriginalWord())
						.getFields()
						.stream()
						.map(DictField::getId)
						.map(it -> selectClause(field, it))
						.collect(Collectors.toCollection(LinkedHashSet::new)))
				.forEach(selectClauses::addAll);

		selectClauses.addAll(
				requiredFields.stream()
						.filter(it -> !"*".equals(it.getWordFieldId().getOriginalWord()))
						.map(this::selectClause)
						.collect(Collectors.toCollection(LinkedHashSet::new))
		);

		if (postgresPageable != null && postgresPageable.isPaged())
		{
			selectClauses.addAll(
					postgresPageable.getPostgresSort()
							.getPostgresOrders()
							.stream()
							.map(PostgresOrder::getPostgresDictFieldName)
							.map(PostgresDictFieldName::getWordJoint)
							.collect(Collectors.toCollection(LinkedHashSet::new))
			);
		}
	}

	public void addJoinClauses(HashSet<String> joinClauses, List<PostgresDictFieldName> requiredFields,
	                           PostgresQueryContext queryContext, PostgresPageable postgresPageable, String dictId)
	{
		var dictRefMap = dictService.getReferenceFieldMap(dictId);

		if (dictRefMap.isEmpty())
		{
			return;
		}

		requiredFields.stream()
				.map(PostgresDictFieldName::getWordDictId)
				.map(PostgresWord::getOriginalWord)
				.filter(dictRefMap::containsKey)
				.map(it -> joinClause(dictId, dictRefMap.get(it).getId(),
						dictRefMap.get(it).getDictRef().getDictId(), dictRefMap.get(it).getDictRef().getFieldId()))
				.forEach(joinClauses::add);

		if (postgresPageable != null && postgresPageable.isPaged())
		{
			postgresPageable.getPostgresSort()
					.getPostgresOrders()
					.stream()
					.map(PostgresOrder::getPostgresDictFieldName)
					.map(PostgresDictFieldName::getWordDictId)
					.map(PostgresWord::getOriginalWord)
					.filter(dictRefMap::containsKey)
					.map(it -> joinClause(dictId, dictRefMap.get(it).getId(),
							dictRefMap.get(it).getDictRef().getDictId(), dictRefMap.get(it).getDictRef().getFieldId()))
					.forEach(joinClauses::add);
		}

		//TODO: провалидировать refDictId's указанные в queryContext на наличие в Dict
		queryContext.getParticipantDictIds()
				.stream()
				.map(PostgresWord::getOriginalWord)
				.filter(dictRefMap::containsKey)
				.map(it -> joinClause(dictId, dictRefMap.get(it).getId(),
						dictRefMap.get(it).getDictRef().getDictId(), dictRefMap.get(it).getDictRef().getFieldId()))
				.forEach(joinClauses::add);
	}

	public void addWhereClauses(HashSet<String> whereClauses, PostgresQueryContext queryContext)
	{
		if (queryContext.getSqlExpression() != null)
		{
			whereClauses.add(queryContext.getSqlExpression());
		}
	}

	public void addOrderByClauses(LinkedHashSet<String> orderByClauses, PostgresPageable postgresPageable)
	{
		if (postgresPageable != null && postgresPageable.isPaged())
		{
			//TODO: провалидировать field's указанные в sort на наличие в dict/refDict
			var orders = postgresPageable.getPostgresSort()
					.getPostgresOrders()
					.stream()
					.map(it -> String.join(" ", it.getPostgresDictFieldName().getWordJoint(), it.getDirection().name()))
					.toList();

			orderByClauses.addAll(orders);
		}
	}

	private void addSelectIdClause(HashSet<String> selectClauses, List<PostgresDictFieldName> requiredFields,
	                               PostgresQueryContext queryContext, PostgresPageable postgresPageable, String dictId)
	{
		if (dictService.getReferenceFieldMap(dictId).isEmpty())
		{
			return;
		}

		requiredFields.stream()
				.filter(it -> !dictId.equals(it.getWordDictId().getOriginalWord()))
				.map(it -> selectClause(it, "id"))
				.forEach(selectClauses::add);

		queryContext.getParticipantDictIds()
				.stream()
				.filter(it -> !dictId.equals(it.getOriginalWord()))
				.map(it -> selectClause(it.getQuotedIfKeyword(), it.getOriginalWord(), "id"))
				.forEach(selectClauses::add);

		postgresPageable.getPostgresSort()
				.getPostgresOrders()
				.stream()
				.map(PostgresOrder::getPostgresDictFieldName)
				.filter(it -> !dictId.equals(it.getWordDictId().getOriginalWord()))
				.map(it -> selectClause(it, "id"))
				.forEach(selectClauses::add);
	}

	private String selectClause(PostgresDictFieldName fieldName)
	{
		return selectClause(fieldName.getWordDictId().getQuotedIfKeyword(),
				fieldName.getWordDictId().getOriginalWord(),
				fieldName.getWordFieldId().getOriginalWord());
	}

	private String selectClause(PostgresDictFieldName fieldName, String dictFieldId)
	{
		return selectClause(fieldName.getWordDictId().getQuotedIfKeyword(),
				fieldName.getWordDictId().getOriginalWord(),
				dictFieldId);
	}

	private String selectClause(String maybeQuotedDictId, String originalDictId, String originalFieldId)
	{
		return SELECT_CLAUSE.formatted(maybeQuotedDictId, originalFieldId, originalDictId, originalFieldId);
	}

	private String joinClause(String dictId, String fieldId, String refDictId, String refFieldId)
	{
		return JOIN_CLAUSE.formatted(refDictId, dictId, fieldId, refDictId, refFieldId);
	}
}
