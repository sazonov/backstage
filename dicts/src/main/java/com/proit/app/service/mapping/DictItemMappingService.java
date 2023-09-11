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

package com.proit.app.service.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proit.app.domain.DictField;
import com.proit.app.domain.DictFieldType;
import com.proit.app.domain.DictItem;
import com.proit.app.model.dictitem.DictDataItem;
import com.proit.app.model.other.date.DateConstants;
import com.proit.app.service.DictService;
import com.proit.app.utils.StreamCollectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.proit.app.constant.ServiceFieldConstants.ID;
import static com.proit.app.constant.ServiceFieldConstants._ID;

@Service
@RequiredArgsConstructor
public class DictItemMappingService
{
	private final ObjectMapper objectMapper;

	private final DictService dictService;

	public DictItem mapDictItem(DictDataItem dataItem)
	{
		var dict = dictService.getById(dataItem.getDictId());

		var requiredFields = dict.getFields()
				.stream()
				.peek(it -> it.setId(it.getId().equals(_ID) ? ID : it.getId()))
				.collect(Collectors.toMap(DictField::getId, Function.identity()));

		var dictDataFields = dictService.getDataFieldsByDict(dict)
				.stream()
				.map(DictField::getId)
				.collect(Collectors.toSet());

		var result = new HashMap<>(dataItem.getDataItemMap())
				.entrySet()
				.stream()
				.peek(entry -> {
					var value = mapField(requiredFields.get(entry.getKey()), entry.getValue());
					entry.setValue(value);
				})
				.collect(StreamCollectors.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));

		var dictData = result.entrySet()
				.stream()
				.filter(it -> dictDataFields.contains(it.getKey()))
				.collect(StreamCollectors.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));

		return DictItem.builder()
				.id(result.containsKey(ID) ? (String) result.get(ID) : null)
				.data(dictData)
				.build();
	}

	private Object mapField(DictField field, Object value)
	{
		if (field.isMultivalued())
		{
			return value == null ? Collections.emptyList() : mapMultivaluedField(field, value);
		}

		return value == null ? null : mapSingleField(field, value);
	}

	private Object mapMultivaluedField(DictField field, Object o)
	{
		if (o instanceof List<?> list)
		{
			return list.stream()
					.map(it -> mapSingleField(field, it))
					.toList();
		}

		return List.of(mapSingleField(field, o));
	}

	private Object mapSingleField(DictField field, Object o)
	{
		if (field.getType() == DictFieldType.INTEGER && o instanceof Double d)
		{
			return d.longValue();
		}

		if (field.getType() == DictFieldType.INTEGER && o instanceof Integer i)
		{
			return i.longValue();
		}

		if (field.getType() == DictFieldType.DECIMAL && o instanceof Integer i)
		{
			return BigDecimal.valueOf(i);
		}

		if (field.getType() == DictFieldType.DECIMAL && o instanceof Double d)
		{
			return BigDecimal.valueOf(d);
		}

		if (field.getType() == DictFieldType.JSON && o instanceof String s)
		{
			try
			{
				return objectMapper.readValue(s, Map.class);
			}
			catch (JsonProcessingException e)
			{
				throw new RuntimeException("Некорректный формат json поля.");
			}
		}

		if (field.getType() != DictFieldType.TIMESTAMP && field.getType() != DictFieldType.DATE)
		{
			return o;
		}

		return field.getType() == DictFieldType.TIMESTAMP
				? mapToLocalDateTime(o)
				: mapToLocalDate(o);
	}

	private LocalDate mapToLocalDate(Object value)
	{
		if (value instanceof Date date)
		{
			return mapToLocalDate(date);
		}
		else if (value instanceof LocalDate localDate)
		{
			return localDate;
		}

		return LocalDate.parse((String) value, DateConstants.DATE_FORMATTER);
	}

	private LocalDate mapToLocalDate(Date value)
	{
		return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

	private LocalDateTime mapToLocalDateTime(Object value)
	{
		if (value instanceof Date date)
		{
			return mapToLocalDateTime(date);
		}
		else if (value instanceof LocalDateTime localDateTime)
		{
			return localDateTime;
		}

		return LocalDateTime.parse((String) value, DateConstants.DATE_TIME_FORMATTER);
	}

	private LocalDateTime mapToLocalDateTime(Date value)
	{
		return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	}
}
