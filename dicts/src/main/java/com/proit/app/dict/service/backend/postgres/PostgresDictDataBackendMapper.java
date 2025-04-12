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
import com.proit.app.dict.domain.DictField;
import com.proit.app.dict.domain.DictItem;
import com.proit.app.dict.exception.dict.field.FieldValidationException;
import com.proit.app.dict.model.dictitem.DictItemColumnName;
import com.proit.app.dict.model.postgres.backend.PostgresDictItem;
import com.proit.app.dict.service.DictService;
import com.proit.app.dict.service.backend.DictDataBackendMapper;
import com.proit.app.utils.DateUtils;
import com.proit.app.utils.JsonUtils;
import com.proit.app.utils.StreamCollectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.lang3.StringUtils;
import org.geojson.GeoJsonObject;
import org.postgresql.jdbc.PgArray;
import org.postgresql.util.PGobject;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostgresDictDataBackendMapper implements DictDataBackendMapper<PostgresDictItem>
{
	private final DictService dictService;

	private final PostgresReservedKeyword reservedKeyword;

	@Override
	public PostgresDictItem mapTo(String dictId, DictItem dictItem)
	{
		var dataWordMap = dictItem.getData()
				.keySet()
				.stream()
				.map(reservedKeyword::postgresWordMap)
				.map(Map::entrySet)
				.flatMap(Collection::stream)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		var postgresDictData = dictItem.getData()
				.entrySet()
				.stream()
				.collect(StreamCollectors.toLinkedHashMap(it -> dataWordMap.get(it.getKey()).getQuotedIfKeyword(), Map.Entry::getValue));

		return new PostgresDictItem(
				reservedKeyword.postgresWordMap(dictId).get(dictId).getQuotedIfKeyword(),
				dictItem.getId(),
				postgresDictData,
				dictItem.getHistory(),
				dictItem.getVersion(),
				timestamp(dictItem.getCreated()),
				timestamp(dictItem.getUpdated()),
				timestamp(dictItem.getDeleted()),
				dictItem.getDeletionReason()
		);
	}

	@Override
	public DictItem mapFrom(String dictId, PostgresDictItem source)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public DictItem mapFromUsingAliases(String dictId, PostgresDictItem source, BidiMap<String, String> dictAliasesRelation)
	{
		try
		{
			if (source == null)
			{
				return null;
			}

			var dataFieldMap = dictService.getDataFieldsByDictId(dictId)
					.stream()
					.collect(Collectors.toMap(it -> it.getId().toLowerCase(), Function.identity()));

			var refFieldMap = dataFieldMap.values()
					.stream()
					.filter(it -> it.getType() == DictFieldType.DICT)
					.collect(Collectors.toMap(it -> it.getId().toLowerCase(), Function.identity()));

			var dataItemMap = source.getDictData()
					.entrySet()
					.stream()
					.collect(Collectors.groupingBy(it -> computeDictId(dictAliasesRelation, it.getKey()),
							StreamCollectors.toLinkedHashMap(it -> computeFieldId(it.getKey()), Map.Entry::getValue)));

			var mappedDictData = mappedDataItems(dataItemMap, dataFieldMap, refFieldMap, dictAliasesRelation);

			return DictItem.builder()
					.id(source.getId())
					.data(mappedDictData)
					.version(source.getVersion())
					.history(source.getHistory())
					.created(DateUtils.toLocalDateTime(source.getCreated()))
					.updated(DateUtils.toLocalDateTime(source.getUpdated()))
					.deleted(DateUtils.toLocalDateTime(source.getDeleted()))
					.deletionReason(source.getDeletionReason())
					.build();
		}
		catch (Exception e)
		{
			throw new RuntimeException("При маппинге DictItem справочника '%s' произошла ошибка: %s".formatted(dictId, e));
		}
	}

	private String computeDictId(BidiMap<String, String> dictAliasesRelation, String placeholder)
	{
		var dictAlias = StringUtils.substringBefore(placeholder, "__");

		return dictAliasesRelation.getKey(dictAlias);
	}

	private String computeFieldId(String placeholder)
	{
		return StringUtils.substringAfter(placeholder, "__");
	}

	private Map<String, Object> mappedDataItems(Map<String, Map<String, Object>> dataItemMap, Map<String, DictField> dataFieldMap,
	                                            Map<String, DictField> refFieldMap, BidiMap<String, String> dictAliasesRelation)
	{
		var resultData = new HashMap<String, Object>();

		var allFieldSelected = dataItemMap.size() == 1;

		dataItemMap.forEach((id, dictData) -> {
			if (refFieldMap.containsKey(id))
			{
				var refDictId = refFieldMap.get(id)
						.getDictRef()
						.getDictId();
				var refDictAlias = dictAliasesRelation.get(refDictId.toLowerCase());
				var refDataMap = dictData.entrySet()
						.stream()
						.collect(StreamCollectors.toLinkedHashMap(it -> "%s__%s".formatted(refDictAlias, it.getKey()), Map.Entry::getValue));

				var refDictItem = mapFromUsingAliases(refDictId, new PostgresDictItem(refDictId, refDataMap, refDictAlias), dictAliasesRelation);

				resultData.put(refDictId, refDictItem);

				return;
			}

			var mappedData = dictData.entrySet()
					.stream()
					.filter(it -> allFieldSelected || !refFieldMap.containsKey(it.getKey()))
					.filter(it -> !DictItemColumnName.SERVICE_COLUMNS.contains(it.getKey()))
					.collect(StreamCollectors.toLinkedHashMap(it -> dataFieldMap.get(it.getKey()).getId(),
							it -> mapValue(it.getValue(), dataFieldMap.get(it.getKey()))));

			resultData.putAll(mappedData);
		});

		return resultData;
	}

	private Object mapValue(Object value, DictField field)
	{
		if (field.isMultivalued())
		{
			return mapMultipleValue(field, value);
		}

		return mapSingleValue(value, field);
	}

	private Object mapMultipleValue(DictField field, Object value)
	{
		try
		{
			if (field.getType() == DictFieldType.JSON)
			{
				var jsonb = (PGobject) value;

				if (jsonb == null || jsonb.getValue() == null)
				{
					return Collections.emptyList();
				}

				return JsonUtils.toList(jsonb.getValue())
						.stream()
						.map(Collections::unmodifiableMap)
						.toList();
			}

			var array = (PgArray) value;

			return array == null ? Collections.emptyList() : Arrays.stream((Object[]) array.getArray())
					.map(it -> mapSingleValue(it, field))
					.toList();
		}
		catch (SQLException e)
		{
			throw new RuntimeException("При маппинге значения '%s', произошла ошибка: %s".formatted(value, e));
		}
	}

	private Object mapSingleValue(Object value, DictField field)
	{
		if (DictFieldType.JSON.equals(field.getType()))
		{
			return value == null ? Collections.emptyMap() : JsonUtils.toObject(value);
		}

		if (DictFieldType.GEO_JSON.equals(field.getType()))
		{
			return value == null ? null : JsonUtils.toObject(value, GeoJsonObject.class);
		}

		if (value instanceof Date date)
		{
			return switch (field.getType())
					{
						case DATE -> DateUtils.toLocalDate(date);
						case TIMESTAMP -> DateUtils.toLocalDateTime(date);
						default -> throw new FieldValidationException("Невозможно привести поле %s к типу %s.".formatted(field.getId(), field.getType().name()));
					};
		}

		return value;
	}

	private Timestamp timestamp(LocalDateTime value)
	{
		return value == null ? null : Timestamp.valueOf(value);
	}
}
