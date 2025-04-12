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

package com.proit.app.dict.service.backend.postgres.clause;

import com.proit.app.dict.api.domain.DictFieldType;
import com.proit.app.dict.constant.ServiceFieldConstants;
import com.proit.app.dict.domain.DictField;
import com.proit.app.dict.service.DictService;
import com.proit.app.dict.service.backend.postgres.PostgresReservedKeyword;
import com.proit.app.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostgresDictDataUpdateClause
{
	private final DictService dictService;

	private final PostgresReservedKeyword reservedKeyword;

	public void addUpdateClause(String column, Object oldValue, Object newValue, LinkedHashSet<String> updateClauses)
	{
		if (!Objects.equals(oldValue, newValue))
		{
			updateClauses.add("%s = %s".formatted(column, newValue == null ? null : "'%s'".formatted(newValue)));
		}
	}

	public void addUpdateJsonClause(String column, Object oldValue, Object newValue, LinkedHashSet<String> updateClauses)
	{
		if (!Objects.equals(oldValue, newValue))
		{
			updateClauses.add("%s = %s".formatted(column, newValue == null ? "'[]'::jsonb" : "'%s'::jsonb".formatted(JsonUtils.toJson(newValue))));
		}
	}

	public void addDictDataUpdateClause(String dictId, Map<String, Object> oldData, Map<String, Object> newData, LinkedHashSet<String> updateClauses)
	{
		var dictDataFields = dictService.getDataFieldsByDictId(dictId);

		var dataWordMap = dictDataFields.stream()
				.map(DictField::getId)
				.map(reservedKeyword::postgresWordMap)
				.map(Map::entrySet)
				.flatMap(Collection::stream)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		var fieldMap = dictDataFields.stream()
				.collect(Collectors.toMap(it -> dataWordMap.get(it.getId()).getQuotedIfKeyword(), Function.identity()));

		newData.entrySet()
				.stream()
				.filter(it -> !StringUtils.equals(it.getKey(), ServiceFieldConstants.ID))
				.forEach(it -> {
					var field = fieldMap.get(it.getKey());

					if (field.isMultivalued())
					{
						completeMultiValue(field, it.getKey(), oldData.getOrDefault(it.getKey(), null), it.getValue(), updateClauses);

						return;
					}

					completeSingleValue(field, it.getKey(), oldData.getOrDefault(it.getKey(), null), it.getValue(), updateClauses);
				});
	}

	private void completeMultiValue(DictField field, String column, Object oldValue, Object newValue, LinkedHashSet<String> updateClauses)
	{
		if (DictFieldType.JSON.equals(field.getType()))
		{
			completeSingleValue(field, column, oldValue, newValue, updateClauses);

			return;
		}

		var arrayOldData = arrayData(oldValue);
		var arrayNewData = arrayData(newValue);

		completeSingleValue(field, column, arrayOldData, arrayNewData, updateClauses);
	}

	private void completeSingleValue(DictField field, String column, Object oldValue, Object newValue, LinkedHashSet<String> updateClauses)
	{
		if (DictFieldType.JSON.equals(field.getType()))
		{
			addUpdateJsonClause(column, oldValue, newValue, updateClauses);

			return;
		}

		addUpdateClause(column, oldValue, newValue, updateClauses);
	}

	private String arrayData(Object value)
	{
		return Optional.ofNullable(value)
				.stream()
				.flatMap(it -> ((List<?>) it).stream())
				.map(JsonUtils::toJson)
				.collect(Collectors.joining(", ", "{", "}"));
	}
}
