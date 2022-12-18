/*
 *    Copyright 2019-2022 the original author or authors.
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

package com.proit.app.service;

import com.proit.app.constant.ServiceFieldConstants;
import com.proit.app.domain.DictField;
import com.proit.app.domain.DictFieldName;
import com.proit.app.domain.DictFieldType;
import com.proit.app.domain.DictItem;
import com.proit.app.utils.StreamCollectors;
import lombok.RequiredArgsConstructor;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.proit.app.constant.ServiceFieldConstants.*;

@Service
@RequiredArgsConstructor
public class MappingService
{
	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
	public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final DictService dictService;

	public DictFieldName mapDictFieldName(String dictField)
	{
		if (dictField.contains("."))
		{
			String[] dictFieldArr = dictField.split("\\.");

			return new DictFieldName(dictFieldArr[0], dictFieldArr[1]);
		}

		return new DictFieldName(null, dictField);
	}

	public DictItem mapDictItem(String dictId, Object o)
	{
		// TODO: почему сразу не мэпить в энтити без ручных кастов?
		if (o == null)
		{
			return null;
		}

		var map = (Map<String, Object>) o;

		var fields = dictService.getDataFieldsByDictId(dictId);

		var availableFieldIds = fields.stream()
				.map(DictField::getId)
				.collect(Collectors.toSet());

		var dictFieldMap = fields.stream().collect(Collectors.toMap(DictField::getId, Function.identity()));

		var refFieldIds = fields.stream()
				.filter(it -> it.getType() == DictFieldType.DICT)
				.map(DictField::getId)
				.collect(Collectors.toSet());

		var data = map.entrySet()
				.stream()
				.filter(entry -> !ServiceFieldConstants.getServiceSchemeFields().contains(entry.getKey()))
				.filter(entry -> availableFieldIds.contains(entry.getKey()))
				.peek(entry ->
						entry.setValue(refFieldIds.contains(entry.getKey()) && entry.getValue() instanceof Map
								? mapDictItem(dictFieldMap.get(entry.getKey()).getDictRef().getDictId(), entry.getValue())
								: castDictField(entry.getValue())))
				.collect(StreamCollectors.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));

		var created = (Date) map.get(CREATED);
		var updated = (Date) map.get(UPDATED);
		var deleted = (Date) map.get(DELETED);
		var deletionReason = (String) map.get(DELETION_REASON);

		String id = null;

		if (map.get(_ID) != null)
		{
			id = map.get(_ID) instanceof String s ? s : ((ObjectId) map.get(_ID)).toString();
		}

		return DictItem.builder()
				.id(id)
				.version((Long) map.get(VERSION))
				.history((List<Map<String, Object>>) map.get(HISTORY))
				.created(created == null ? null : mapToLocalDateTime(created))
				.deleted(deleted == null ? null : mapToLocalDateTime(deleted))
				.deletionReason(deleted == null ? null : deletionReason)
				.updated(updated == null ? null : mapToLocalDateTime(updated))
				.data(data)
				.build();
	}

	public Map<String, Object> mapDictDoc(String dictId, Map<String, Object> doc)
	{
		var requiredFields = dictService.getById(dictId)
				.getFields()
				.stream()
				.peek(it -> it.setId(it.getId().equals(_ID) ? ID : it.getId()))
				.collect(Collectors.toMap(DictField::getId, Function.identity()));

		var result = new HashMap<>(doc).entrySet()
				.stream()
				.peek(entry -> {
					var value = entry.getValue() == null ? null : mapField(requiredFields.get(entry.getKey()), entry.getValue());
					entry.setValue(value);
				})
				.collect(StreamCollectors.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));

		if (result.containsKey(ID))
		{
			result.put(_ID, result.get(ID));
			result.remove(ID);
		}

		return result;
	}

	private Object mapField(DictField field, Object o)
	{
		if (field.isMultivalued())
		{
			return mapMultivaluedField(field, o);
		}

		return mapSingleField(field, o);
	}

	private Object mapMultivaluedField(DictField field, Object o)
	{
		if (o instanceof List<?> list)
		{
			return list.stream()
					.map(it -> mapSingleField(field, it))
					.toList();
		}

		var list = new ArrayList<>();
		list.add(mapSingleField(field, o));

		return list;
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
			return Decimal128.parse(String.valueOf(i.doubleValue()));
		}

		if (field.getType() == DictFieldType.DECIMAL && o instanceof BigDecimal bd)
		{
			return new Decimal128(bd);
		}

		if (field.getType() == DictFieldType.DECIMAL && o instanceof Double d)
		{
			return Decimal128.parse(String.valueOf(d));
		}

		if (field.getType() != DictFieldType.TIMESTAMP && field.getType() != DictFieldType.DATE)
		{
			return o;
		}

		return field.getType() == DictFieldType.TIMESTAMP
				? mapToLocalDateTime(o)
				: mapToLocalDate(o);
	}

	/**
	 * Для Decimal поля конвертируем исходный тип в BigDecimal
	 *
	 */
	private Object castDictField(Object o)
	{
		if (o instanceof Decimal128 decimal128)
		{
			return decimal128.bigDecimalValue();
		}

		return o;
	}

	private LocalDate mapToLocalDate(Object value)
	{
		if (value instanceof Date date)
		{
			return mapToLocalDate(date);
		}

		return LocalDate.parse((String) value, DATE_FORMATTER);
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

		return LocalDateTime.parse((String) value, DATE_TIME_FORMATTER);
	}

	private LocalDateTime mapToLocalDateTime(Date value)
	{
		return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	}
}