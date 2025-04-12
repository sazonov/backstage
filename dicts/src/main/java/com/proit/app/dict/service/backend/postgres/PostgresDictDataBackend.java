/*
 *    Copyright 2019-2024 the original author or authors.
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

package com.proit.app.dict.service.backend.postgres;

import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.domain.DictFieldName;
import com.proit.app.dict.domain.DictItem;
import com.proit.app.dict.exception.dictitem.DictItemCreatedException;
import com.proit.app.dict.exception.dictitem.DictItemDeletedException;
import com.proit.app.dict.exception.dictitem.DictItemUpdatedException;
import com.proit.app.dict.model.dictitem.DictItemColumnName;
import com.proit.app.dict.model.postgres.backend.PostgresDictFieldName;
import com.proit.app.dict.model.postgres.backend.PostgresDictItem;
import com.proit.app.dict.model.postgres.backend.PostgresPageable;
import com.proit.app.dict.model.postgres.query.PostgresQueryContext;
import com.proit.app.dict.service.backend.DictDataBackend;
import com.proit.app.dict.service.backend.Engine;
import com.proit.app.dict.service.backend.postgres.clause.PostgresDictDataInsertClause;
import com.proit.app.dict.service.backend.postgres.clause.PostgresDictDataQueryClause;
import com.proit.app.dict.service.backend.postgres.clause.PostgresDictDataUpdateClause;
import com.proit.app.dict.service.query.PostgresTranslator;
import com.proit.app.dict.service.query.QueryParser;
import com.proit.app.dict.service.query.ast.QueryExpression;
import com.proit.app.exception.ObjectNotFoundException;
import com.proit.app.utils.DataUtils;
import com.proit.app.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostgresDictDataBackend extends AbstractPostgresBackend implements DictDataBackend
{
	private final QueryParser queryParser;
	private final PostgresTranslator postgresTranslator;

	private final PostgresDictDataInsertClause insertClause;
	private final PostgresDictDataUpdateClause updateClause;
	private final PostgresDictDataQueryClause filterClause;

	private final PostgresPageableMapper pageableMapper;
	private final PostgresDictFieldNameMapper fieldNameMapper;
	private final PostgresDictDataBackendMapper dataBackendMapper;

	@Override
	public Engine getEngine()
	{
		return postgresEngine;
	}

	@Override
	public DictItem getById(Dict dict, String id, List<DictFieldName> requiredFields)
	{
		var query = "%s = '%s'".formatted(DictItemColumnName.ID.getName(), id);

		return getByFilter(dict, requiredFields, queryParser.parse(query), Pageable.unpaged())
				.stream()
				.findFirst()
				.orElseThrow(() -> new ObjectNotFoundException(DictItem.class, "dictId: %s, itemId: %s".formatted(dict.getId(), id)));
	}

	@Override
	public List<DictItem> getByIds(Dict dict, List<String> ids, List<DictFieldName> requiredFields)
	{
		var itemIds = ids.stream()
				.map(it -> "'" + it + "'")
				.collect(Collectors.joining(", "));

		var filtersQuery = "%s in (%s)".formatted(DictItemColumnName.ID.getName(), itemIds);

		return getByFilter(dict, requiredFields, queryParser.parse(filtersQuery), Pageable.unpaged()).getContent();
	}

	@Override
	public Page<DictItem> getByFilter(Dict dict, List<DictFieldName> requiredFields, QueryExpression queryExpression, Pageable pageable)
	{
		var dictId = dict.getId();

		var dictAliasesRelation = new DualHashBidiMap<String, String>();
		var selectClauses = new LinkedHashSet<String>();
		var joinClauses = new LinkedHashSet<String>();
		var whereClauses = new LinkedHashSet<String>();
		var orderByClauses = new LinkedHashSet<String>();
		var postgresPageable = pageableMapper.mapFrom(dictId, pageable);

		var query = postgresTranslator.process(dict, queryExpression);

		completeFilterClauses(dictAliasesRelation, selectClauses, joinClauses, whereClauses, orderByClauses, query,
				fieldNameMapper.mapFrom(dictId, requiredFields), postgresPageable, dictId);

		var wordDictId = wordMap(dictId).get(dictId).getQuotedIfKeyword();

		var sqlIds = "select " + (joinClauses.isEmpty() ? "" : "distinct ") + String.join(", ", selectClauses)
				+ " from " + wordDictId + (joinClauses.isEmpty() ? "" : " " + String.join(" ", joinClauses))
				+ (whereClauses.isEmpty() ? "" : " where " + String.join(" and ", whereClauses))
				+ (orderByClauses.isEmpty() ? "" : " order by " + String.join(", ", orderByClauses))
				+ (postgresPageable.isPaged() ? " limit " + postgresPageable.getPageSize() + " offset " + postgresPageable.getOffset() : "");

		var countSql = "select count(" + (joinClauses.isEmpty() ? "" : "distinct ") + wordDictId + ".id) from " + wordDictId
				+ (joinClauses.isEmpty() ? "" : " " + String.join(" ", joinClauses))
				+ (whereClauses.isEmpty() ? "" : " where " + String.join(" and ", whereClauses));

		var postgresItems = jdbc.queryForList(sqlIds, Map.of())
				.stream()
				.map(it -> new PostgresDictItem(dictId, it, dictAliasesRelation.get(dictId.toLowerCase())))
				.toList();

		var dictItems = dataBackendMapper.mapFromUsingAliases(dictId, postgresItems, dictAliasesRelation);

		if (dictItems.isEmpty())
		{
			return DataUtils.emptyPage(pageable);
		}

		var count = Objects.requireNonNull(jdbc.queryForObject(countSql, Map.of(), Long.class));

		return new PageImpl<>(dictItems, pageable, count);
	}

	@Override
	public boolean existsById(Dict dict, String itemId)
	{
		var dictId = dict.getId();

		var parameterMap = Map.of(
				"dictId", wordMap(dictId).get(dictId).getQuotedIfKeyword().toLowerCase(),
				"itemId", itemId
		);

		var sql = sqlWithParameters("select exists(select 1 from ${dictId} where id = '${itemId}')", parameterMap);

		return Boolean.TRUE.equals(jdbc.queryForObject(sql, parameterMap, Boolean.class));
	}

	@Override
	public boolean existsByFilter(Dict dict, QueryExpression queryExpression)
	{
		return getByFilter(dict, List.of(new DictFieldName(null, "*")), queryExpression, Pageable.unpaged())
				.getContent()
				.stream()
				.findFirst()
				.isPresent();
	}

	@Override
	public DictItem create(Dict dict, DictItem dictItem)
	{
		addTransactionData(dict, false);

		return transactionWithResult(() -> createItem(dict, dictItem), dict.getId(), DictItemCreatedException::new);
	}

	@Override
	public List<DictItem> createMany(Dict dict, List<DictItem> dictItems)
	{
		addTransactionData(dict, false);

		var dictId = dict.getId();

		return transactionWithResult(() -> createItems(dict, dictItems), dictId, DictItemCreatedException::new);
	}

	@Override
	public DictItem update(Dict dict, String itemId, DictItem dictItem, long version)
	{
		addTransactionData(dict, false);

		var dictId = dict.getId();

		return transactionWithResult(() -> updateItem(dict, itemId, dictItem), dictId, itemId, DictItemUpdatedException::new);
	}

	@Override
	public void delete(Dict dict, DictItem dictItem)
	{
		addTransactionData(dict, false);

		transactionWithoutResult(() -> deleteItem(dict, dictItem), dict.getId(), dictItem.getId(), DictItemDeletedException::new);
	}

	@Override
	public void deleteAll(Dict dict, List<DictItem> dictItems)
	{
		addTransactionData(dict, false);

		transactionWithoutResult(() -> deleteItems(dict, dictItems), dict.getId(), String.join(", ", dictItems.stream().map(DictItem::getId).toList()), DictItemDeletedException::new);
	}

	@Override
	public long countByFilter(Dict dict, QueryExpression queryExpression)
	{
		return getByFilter(dict, List.of(new DictFieldName(null, "*")), queryExpression, Pageable.unpaged())
				.getTotalElements();
	}

	private DictItem createItem(Dict dict, DictItem dictItem)
	{
		var values = new LinkedList<>();
		var columns = new LinkedHashSet<String>();

		dictItem.setId(dictItem.getId() == null ? String.valueOf(UUID.randomUUID()) : dictItem.getId());

		var dictId = dict.getId();

		completeInsertClauses(columns, values, dictId, dataBackendMapper.mapTo(dictId, dictItem));

		var sqlValues = values.stream()
				.map(it -> it == null ? null : it.toString())
				.collect(Collectors.joining(", "));

		var parameterMap = Map.of(
				"dictId", wordMap(dictId).get(dictId).getQuotedIfKeyword(),
				"columns", String.join(", ", columns),
				"values", sqlValues
		);

		var sql = sqlWithParameters("insert into ${dictId} (${columns}) values (${values})", parameterMap);

		jdbc.update(sql, new EmptySqlParameterSource());

		return getById(dict, dictItem.getId(), List.of(new DictFieldName(null, "*")));
	}

	private List<DictItem> createItems(Dict dict, List<DictItem> dictItems)
	{
		var sqlValues = new LinkedList<String>();
		var columns = new LinkedHashSet<String>();

		var dictId = dict.getId();

		dictItems.forEach(it -> {
			var valueClauses = new LinkedList<>();

			it.setId(it.getId() == null ? String.valueOf(UUID.randomUUID()) : it.getId());

			completeInsertClauses(columns, valueClauses, dictId, dataBackendMapper.mapTo(dictId, it));

			var values = valueClauses.stream()
					.map(value -> value == null ? null : value.toString())
					.collect(Collectors.joining(", ", "(", ")"));

			sqlValues.add(values);
		});

		var parameterMap = Map.of(
				"dictId", wordMap(dictId).get(dictId).getQuotedIfKeyword(),
				"columns", String.join(", ", columns),
				"values", String.join(", ", sqlValues)
		);

		var sql = sqlWithParameters("insert into ${dictId} (${columns}) values ${values}", parameterMap);

		jdbc.update(sql, new EmptySqlParameterSource());

		var itemIds = dictItems.stream()
				.map(DictItem::getId)
				.collect(Collectors.toList());

		return getByIds(dict, itemIds, List.of(new DictFieldName(null, "*")));
	}

	private DictItem updateItem(Dict dict, String itemId, DictItem dictItem)
	{
		var updateClauses = new LinkedHashSet<String>();

		var dictId = dict.getId();

		completeUpdateClause(updateClauses, dict, itemId, dataBackendMapper.mapTo(dictId, dictItem));

		var parameterMap = Map.of(
				"dictId", wordMap(dictId).get(dictId).getQuotedIfKeyword(),
				"updateClauses", String.join(", ", updateClauses),
				"itemId", itemId
		);

		var sql = sqlWithParameters("update ${dictId} set ${updateClauses} where id = '${itemId}'", parameterMap);

		jdbc.update(sql, new EmptySqlParameterSource());

		return getById(dict, itemId, List.of(new DictFieldName(null, "*")));
	}

	private void deleteItem(Dict dict, DictItem dictItem)
	{
		var updateClauses = new LinkedHashSet<String>();

		var dictId = dict.getId();

		completeUpdateClause(updateClauses, dict, dictItem.getId(), dataBackendMapper.mapTo(dictId, dictItem));

		var parameterMap = Map.of(
				"dictId", wordMap(dictId).get(dictId).getQuotedIfKeyword(),
				"updateClauses", String.join(", ", updateClauses),
				"itemId", dictItem.getId()
		);

		var sql = sqlWithParameters("update ${dictId} set ${updateClauses} where id = '${itemId}'", parameterMap);

		jdbc.update(sql, new EmptySqlParameterSource());
	}

	private void deleteItems(Dict dict, List<DictItem> dictItems)
	{
		var updateBatch = new LinkedHashSet<String>();

		var dictId = dict.getId();

		dictItems.forEach(it -> {
			var clauses = new LinkedHashSet<String>();

			completeUpdateClause(clauses, dict, it.getId(), dataBackendMapper.mapTo(dictId, it));

			var parameterMap = Map.of(
					"dictId", wordMap(dictId).get(dictId).getQuotedIfKeyword(),
					"updateClauses", String.join(", ", clauses),
					"itemId", it.getId()
			);

			updateBatch.add(sqlWithParameters("update ${dictId} set ${updateClauses} where id = '${itemId}'", parameterMap));
		});

		jdbc.update(String.join(";", updateBatch), new EmptySqlParameterSource());
	}

	private void completeInsertClauses(LinkedHashSet<String> columns, LinkedList<Object> values, String dictId, PostgresDictItem postgresDictItem)
	{
		insertClause.addInsertClause(DictItemColumnName.ID.getName(), postgresDictItem.getId(), columns, values);
		insertClause.addDictDataInsertClause(dictId, postgresDictItem, columns, values);
		insertClause.addInsertJsonClause(DictItemColumnName.HISTORY.getName(), postgresDictItem.getHistory(), columns, values);
		insertClause.addInsertClause(DictItemColumnName.VERSION.getName(), postgresDictItem.getVersion(), columns, values);
		insertClause.addInsertClause(DictItemColumnName.CREATED.getName(), postgresDictItem.getCreated(), columns, values);
		insertClause.addInsertClause(DictItemColumnName.UPDATED.getName(), postgresDictItem.getUpdated(), columns, values);
	}

	private void completeUpdateClause(LinkedHashSet<String> updateClauses, Dict dict, String itemId, PostgresDictItem postgresDictItem)
	{
		var oldItem = getById(dict, itemId, List.of(new DictFieldName(null, "*")));

		updateClause.addDictDataUpdateClause(dict.getId(), oldItem.getData(), postgresDictItem.getDictData(), updateClauses);

		updateClauses.add("%s = '%s'".formatted(DictItemColumnName.VERSION.getName(), postgresDictItem.getVersion()));
		updateClauses.add("%s = '%s'::jsonb".formatted(DictItemColumnName.HISTORY.getName(), JsonUtils.toJson(postgresDictItem.getHistory())));
		updateClauses.add("%s = '%s'".formatted(DictItemColumnName.UPDATED.getName(), postgresDictItem.getUpdated()));

		updateClause.addUpdateClause(DictItemColumnName.DELETED.getName(), oldItem.getDeleted(), postgresDictItem.getDeleted(), updateClauses);
		updateClause.addUpdateClause(DictItemColumnName.DELETION_REASON.getName(), oldItem.getDeletionReason(), postgresDictItem.getDeletionReason(), updateClauses);
	}

	private void completeFilterClauses(BidiMap<String, String> dictAliasesRelation, LinkedHashSet<String> selectClauses, LinkedHashSet<String> joinClauses,
	                                   LinkedHashSet<String> whereClauses, LinkedHashSet<String> orderByClauses,
	                                   PostgresQueryContext queryContext, List<PostgresDictFieldName> requiredFields, PostgresPageable postgresPageable, String dictId)
	{
		filterClause.addSelectClauses(dictAliasesRelation, selectClauses, requiredFields, queryContext, postgresPageable, dictId);
		filterClause.addJoinClauses(joinClauses, requiredFields, queryContext, postgresPageable, dictId);
		filterClause.addWhereClauses(whereClauses, queryContext);
		filterClause.addOrderByClauses(orderByClauses, postgresPageable, dictId);
	}
}
