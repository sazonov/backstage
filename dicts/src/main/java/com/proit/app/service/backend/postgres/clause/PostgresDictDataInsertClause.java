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
import com.proit.app.domain.DictFieldType;
import com.proit.app.model.postgres.backend.PostgresDictItem;
import com.proit.app.service.DictService;
import com.proit.app.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostgresDictDataInsertClause
{
	private final DictService dictService;

	public void addInsertClause(String column, Object value, LinkedHashSet<String> columns, LinkedList<Object> values)
	{
		columns.add(column);
		values.add(value == null ? null : "'%s'".formatted(value));
	}

	public void addInsertJsonClause(String columnPlaceholder, Object value, LinkedHashSet<String> columns, LinkedList<Object> values)
	{
		columns.add(columnPlaceholder);
		values.add(value == null ? "'[]'::jsonb" : "'%s'::jsonb".formatted(JsonUtils.asJson(value)));
	}

	public void addDictDataInsertClause(String dictId, PostgresDictItem dictItem, LinkedHashSet<String> columns, LinkedList<Object> values)
	{
		var fieldMap = dictService.getDataFieldsByDictId(dictId)
				.stream()
				.collect(Collectors.toMap(DictField::getId, Function.identity()));

		dictItem.getDictData()
				.forEach((column, value) -> {
					var mapKeyColumn = column.replace("\"", "");

					var field = fieldMap.get(mapKeyColumn);

					if (field.isMultivalued())
					{
						completeMultiValue(field, column, value, columns, values);

						return;
					}

					completeSingleValue(field, column, value, columns, values);
				});
	}

	private void completeMultiValue(DictField field, String column, Object value, LinkedHashSet<String> columns, LinkedList<Object> values)
	{
		if (DictFieldType.JSON.equals(field.getType()))
		{
			completeSingleValue(field, column, value, columns, values);

			return;
		}

		var arrayData = arrayData(value);

		completeSingleValue(field, column, arrayData, columns, values);
	}

	private void completeSingleValue(DictField field, String column, Object value, LinkedHashSet<String> columns, LinkedList<Object> values)
	{
		if (DictFieldType.JSON.equals(field.getType()))
		{
			addInsertJsonClause(column, value, columns, values);

			return;
		}

		addInsertClause(column, value, columns, values);
	}

	private String arrayData(Object value)
	{
		return Optional.ofNullable(value)
				.stream()
				.flatMap(it -> ((List<?>) it).stream())
				.map(JsonUtils::asJson)
				.collect(Collectors.joining(", ", "{", "}"));
	}
}
