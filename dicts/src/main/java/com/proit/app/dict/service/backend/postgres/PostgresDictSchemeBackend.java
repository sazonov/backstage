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

import com.proit.app.dict.api.domain.DictFieldType;
import com.proit.app.dict.configuration.properties.DictsProperties;
import com.proit.app.dict.constant.ServiceFieldConstants;
import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.domain.DictConstraint;
import com.proit.app.dict.domain.DictField;
import com.proit.app.dict.domain.DictIndex;
import com.proit.app.dict.exception.dict.*;
import com.proit.app.dict.exception.dict.constraint.ConstraintCreatedException;
import com.proit.app.dict.exception.dict.constraint.ConstraintDeletedException;
import com.proit.app.dict.exception.dict.field.FieldUpdatedException;
import com.proit.app.dict.exception.dict.index.IndexCreatedException;
import com.proit.app.dict.exception.dict.index.IndexDeletedException;
import com.proit.app.dict.model.postgres.backend.PostgresWord;
import com.proit.app.dict.service.backend.DictBackend;
import com.proit.app.dict.service.backend.DictSchemeBackend;
import com.proit.app.dict.service.backend.Engine;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostgresDictSchemeBackend extends AbstractPostgresBackend implements DictSchemeBackend
{
	private final DictsProperties dictsProperties;

	private final DictBackend dictBackend;

	@Override
	public Engine getEngine()
	{
		return postgresEngine;
	}

	@Override
	public Dict createDictScheme(Dict dict)
	{
		var created = transactionWithResult(() -> createdDictScheme(dict), dict.getId(), DictCreatedException::new);

		addTransactionData(dict, true);

		return created;
	}

	@Override
	public Dict updateDictScheme(Dict updatedDict)
	{
		addTransactionData(updatedDict, true);

		var dictId = updatedDict.getId();

		return transactionWithResult(() -> updatedDictScheme(dictId, updatedDict), dictId, DictUpdatedException::new);
	}

	@Override
	public void renameDictSchemeById(String dictId, String renamedDictId)
	{
		transactionWithoutResult(() -> renameScheme(dictId, renamedDictId), dictId, DictUpdatedException::new);
	}

	@Override
	public void deleteDictSchemeById(String dictId)
	{
		transactionWithoutResult(() -> deleteScheme(dictId), dictId, DictDeletedException::new);
	}

	@Override
	public boolean existsDictSchemeById(String dictId)
	{
		var parameterMap = new MapSqlParameterSource();
		addParameter(parameterMap, "tableName", wordMap(dictId).get(dictId).getQuotedIfKeyword().toLowerCase());
		addParameter(parameterMap, "schemaName", dictsProperties.getDdl().getScheme());

		var existsSql = "select exists(select from pg_tables where tablename = :tableName and schemaname = :schemaName)";

		return Boolean.TRUE.equals(jdbc.queryForObject(existsSql, parameterMap, Boolean.class));
	}

	@Override
	public DictField renameDictField(Dict dict, String oldFieldId, DictField field)
	{
		addTransactionData(dict, true);

		return transactionWithResult(() -> renamedField(dict, oldFieldId, field), dict.getId(), oldFieldId, FieldUpdatedException::new);
	}

	@Override
	public DictConstraint createConstraint(Dict dict, DictConstraint constraint)
	{
		addTransactionData(dict, true);

		return transactionWithResult(() -> createdConstraint(dict, constraint), dict.getId(), ConstraintCreatedException::new);
	}

	@Override
	public void deleteConstraint(Dict dict, String id)
	{
		addTransactionData(dict, true);

		transactionWithoutResult(() -> deleteDictConstraint(dict, id), dict.getId(), id, ConstraintDeletedException::new);
	}

	@Override
	public DictIndex createIndex(Dict dict, DictIndex index)
	{
		addTransactionData(dict, true);

		return transactionWithResult(() -> createdIndex(dict, index), dict.getId(), IndexCreatedException::new);
	}

	@Override
	public void deleteIndex(Dict dict, String id)
	{
		addTransactionData(dict, true);

		transactionWithoutResult(() -> deleteDictIndex(id), dict.getId(), id, IndexDeletedException::new);
	}

	private Dict createdDictScheme(Dict dict)
	{
		if (existsDictSchemeById(dict.getId()))
		{
			throw new DictAlreadyExistsException(dict.getId());
		}

		var fieldsDefinition = dictFieldsWithDefinition(dict.getFields());

		fieldsDefinition.add("primary key (%s)".formatted(ServiceFieldConstants.ID));

		var wordMap = wordMap(dict.getId());

		var parameterMap = Map.of(
				"dictId", wordMap.get(dict.getId()).getQuotedIfKeyword(),
				"definition", String.join(", ", fieldsDefinition)
		);

		var sql = sqlWithParameters("create table ${dictId} (${definition})", parameterMap);

		jdbc.update(sql, new EmptySqlParameterSource());

		return dict;
	}

	private Dict updatedDictScheme(String dictId, Dict updatedDict)
	{
		var actualDict = dictBackend.getDictById(dictId);

		var operations = new ArrayList<String>();

		var actualFieldIds = actualDict.getFields()
				.stream()
				.map(DictField::getId)
				.collect(Collectors.toList());

		var actualDictEngine = actualDict.getEngine();
		var updatedDictEngine = updatedDict.getEngine();

		if (actualDictEngine != null && !actualDictEngine.getName().equals(updatedDictEngine.getName()))
		{
			ServiceFieldConstants.getServiceSchemaFieldsWithoutIds()
					.stream()
					.filter(Predicate.not(actualFieldIds::contains))
					.forEach(actualFieldIds::add);
		}

		var updatedFieldIds = updatedDict.getFields()
				.stream()
				.map(DictField::getId)
				.toList();

		var actualWordMap = wordMap(actualFieldIds, dictId);

		var updatedWordMap = wordMap(updatedFieldIds, dictId);

		var addColumnOperations = updatedDict.getFields()
				.stream()
				.filter(Predicate.not(it -> actualWordMap.containsKey(it.getId())))
				.map(it -> {
					var parameterMap = Map.of(
							"dictId", updatedWordMap.get(dictId).getQuotedIfKeyword(),
							"column", updatedWordMap.get(it.getId()).getQuotedIfKeyword(),
							"definition", computeDefinitionType(it)
					);

					return sqlWithParameters("alter table ${dictId} add column ${column} ${definition}", parameterMap);
				})
				.toList();

		var dropColumnOperations = actualDict.getFields()
				.stream()
				.filter(Predicate.not(it -> updatedWordMap.containsKey(it.getId())))
				.map(it -> {
					var parameterMap = Map.of(
							"dictId", actualWordMap.get(dictId).getQuotedIfKeyword(),
							"column", actualWordMap.get(it.getId()).getQuotedIfKeyword()
					);

					return sqlWithParameters("alter table ${dictId} drop column ${column}", parameterMap);
				})
				.toList();

		operations.addAll(addColumnOperations);
		operations.addAll(dropColumnOperations);

		jdbc.update(String.join(";", operations), new EmptySqlParameterSource());

		return updatedDict;
	}

	private void renameScheme(String dictId, String renamedDictId)
	{
		if (!existsDictSchemeById(dictId))
		{
			throw new DictNotFoundException(dictId);
		}

		var wordMap = wordMap(dictId, renamedDictId);

		var parameterMap = Map.of(
				"oldDictId", wordMap.get(dictId).getQuotedIfKeyword(),
				"newDictId", wordMap.get(renamedDictId).getQuotedIfKeyword()
		);

		var sql = sqlWithParameters("alter table ${oldDictId} rename to ${newDictId}", parameterMap);

		jdbc.update(sql, new EmptySqlParameterSource());
	}

	private void deleteScheme(String dictId)
	{
		if (!existsDictSchemeById(dictId))
		{
			throw new DictNotFoundException(dictId);
		}

		var sql = sqlWithParameters("drop table ${dictId}", Map.of("dictId", wordMap(dictId).get(dictId).getQuotedIfKeyword()));

		jdbc.update(sql, new EmptySqlParameterSource());
	}

	private DictField renamedField(Dict dict, String oldFieldId, DictField field)
	{
		var wordMap = wordMap(dict.getId(), oldFieldId, field.getId());

		var parameterMap = Map.of(
				"dictId", wordMap.get(dict.getId()).getQuotedIfKeyword(),
				"oldColumn", wordMap.get(oldFieldId).getQuotedIfKeyword(),
				"newColumn", wordMap.get(field.getId()).getQuotedIfKeyword()
		);

		var sql = sqlWithParameters("alter table ${dictId} rename column ${oldColumn} to ${newColumn}", parameterMap);

		jdbc.update(sql, new EmptySqlParameterSource());

		return field;
	}

	private DictConstraint createdConstraint(Dict dict, DictConstraint constraint)
	{
		var wordMap = wordMap(constraint.getFields(), constraint.getId(), dict.getId());

		var fields = constraint.getFields()
				.stream()
				.map(wordMap::get)
				.map(PostgresWord::getQuotedIfKeyword)
				.collect(Collectors.joining(", "));

		var parameterMap = Map.of(
				"dictId", wordMap.get(dict.getId()).getQuotedIfKeyword(),
				"constraint", wordMap.get(constraint.getId()).getQuotedIfKeyword(),
				"fields", fields
		);

		var sql = sqlWithParameters("alter table ${dictId} add constraint ${constraint} unique (${fields})", parameterMap);

		jdbc.update(sql, new EmptySqlParameterSource());

		return constraint;
	}

	private void deleteDictConstraint(Dict dict, String id)
	{
		var wordMap = wordMap(dict.getId(), id);

		var parameterMap = Map.of(
				"dictId", wordMap.get(dict.getId()).getQuotedIfKeyword(),
				"constraint", wordMap.get(id).getQuotedIfKeyword()
		);

		var sql = sqlWithParameters("alter table ${dictId} drop constraint ${constraint}", parameterMap);

		jdbc.update(sql, new EmptySqlParameterSource());
	}

	private DictIndex createdIndex(Dict dict, DictIndex index)
	{
		var wordMap = wordMap(index.getFields(), index.getId(), dict.getId());

		var fields = index.getFields()
				.stream()
				.map(wordMap::get)
				.map(PostgresWord::getQuotedIfKeyword)
				.map(it -> it + " " + index.getDirection().name())
				.collect(Collectors.joining(", "));

		var parameterMap = Map.of(
				"indexId", wordMap.get(index.getId()).getQuotedIfKeyword(),
				"dictId", wordMap.get(dict.getId()).getQuotedIfKeyword(),
				"fields", fields
		);

		var sql = sqlWithParameters("create index ${indexId} on ${dictId} (${fields})", parameterMap);

		jdbc.update(sql, new EmptySqlParameterSource());

		return index;
	}

	private void deleteDictIndex(String indexId)
	{
		var sql = sqlWithParameters("drop index ${indexId}", Map.of("indexId", indexId));

		jdbc.update(sql, new EmptySqlParameterSource());
	}

	private List<String> dictFieldsWithDefinition(List<DictField> fields)
	{
		var fieldIds = fields.stream()
				.map(DictField::getId)
				.toList();

		var wordMap = wordMap(fieldIds);

		return fields.stream()
				.map(field -> "%s %s".formatted(wordMap.get(field.getId()).getQuotedIfKeyword(), computeDefinitionType(field)))
				.collect(Collectors.toList());
	}

	private String computeDefinitionType(DictField field)
	{
		var singleType = switch (field.getType())
		{
			case INTEGER -> "bigint";
			case DECIMAL -> "numeric";
			case STRING, DICT, ENUM, ATTACHMENT, GEO_JSON -> "text";
			case BOOLEAN -> "boolean";
			case DATE -> "date";
			case TIMESTAMP -> "timestamp";
			case JSON -> "jsonb default '%s'::jsonb".formatted(field.isMultivalued() ? "[]" : "{}");
		};

		if (field.isMultivalued() && !DictFieldType.JSON.equals(field.getType()))
		{
			singleType += "[]";
		}

		if (field.isRequired())
		{
			singleType += " not null";
		}

		return singleType;
	}
}
