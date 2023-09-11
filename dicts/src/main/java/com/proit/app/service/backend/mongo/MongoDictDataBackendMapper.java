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

package com.proit.app.service.backend.mongo;

import com.proit.app.constant.ServiceFieldConstants;
import com.proit.app.domain.DictField;
import com.proit.app.domain.DictFieldType;
import com.proit.app.domain.DictItem;
import com.proit.app.exception.dict.field.FieldValidationException;
import com.proit.app.service.DictService;
import com.proit.app.service.backend.DictDataBackendMapper;
import com.proit.app.utils.JsonUtils;
import com.proit.app.utils.StreamCollectors;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.geojson.GeoJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.proit.app.constant.ServiceFieldConstants.*;

@Component
public class MongoDictDataBackendMapper implements DictDataBackendMapper<Document>
{
	private final DictService dictService;

	@Autowired
	public MongoDictDataBackendMapper(@Lazy DictService dictService)
	{
		this.dictService = dictService;
	}

	@Override
	public Document mapTo(String dictId, DictItem dictItem)
	{
		var dataFieldsMap = dictService.getDataFieldsByDictId(dictId)
				.stream()
				.collect(Collectors.toMap(DictField::getId, Function.identity()));

		var document = dictItem.getData()
				.entrySet()
				.stream()
				.collect(Document::new,
						(doc, entry) -> doc.append(entry.getKey(), mapDocumentItem(entry.getValue(), dataFieldsMap.get(entry.getKey()))),
						Document::putAll);

		var created = dictItem.getCreated() == null ? null : serviceDateTime(dictItem.getCreated());
		var updated = dictItem.getUpdated() == null ? null : serviceDateTime(dictItem.getUpdated());
		var deleted = dictItem.getDeleted() == null ? null : serviceDateTime(dictItem.getDeleted());

		document.append(_ID, dictItem.getId());
		document.append(VERSION, dictItem.getVersion());
		document.append(HISTORY, dictItem.getHistory());
		document.append(CREATED, created);
		document.append(UPDATED, updated);
		document.append(DELETED, deleted);
		document.append(DELETION_REASON, dictItem.getDeletionReason());

		return document;
	}

	@Override
	public DictItem mapFrom(String dictId, Document source)
	{
		if (source == null)
		{
			return null;
		}

		var dataFieldMap = dictService.getDataFieldsByDictId(dictId)
				.stream()
				.collect(Collectors.toMap(DictField::getId, Function.identity()));

		var refFieldIds = dataFieldMap.values()
				.stream()
				.filter(it -> it.getType() == DictFieldType.DICT)
				.map(DictField::getId)
				.collect(Collectors.toSet());

		var mappedDictData = source.entrySet()
				.stream()
				.filter(entry -> !ServiceFieldConstants.getServiceSchemeFields().contains(entry.getKey()))
				.filter(entry -> dataFieldMap.containsKey(entry.getKey()))
				.peek(entry -> setValue(dataFieldMap, refFieldIds, entry))
				.collect(StreamCollectors.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));

		var created = (Date) source.get(CREATED);
		var updated = (Date) source.get(UPDATED);
		var deleted = (Date) source.get(DELETED);
		var deletionReason = (String) source.get(DELETION_REASON);

		return DictItem.builder()
				.id(source.get(_ID) instanceof String s ? s : ((ObjectId) source.get(_ID)).toString())
				.data(mappedDictData)
				.version((Long) source.get(VERSION))
				.history((List<Map<String, Object>>) source.get(HISTORY))
				.created(created == null ? null : mapToLocalDateTime(created))
				.updated(updated == null ? null : mapToLocalDateTime(updated))
				.deleted(deleted == null ? null : mapToLocalDateTime(deleted))
				.deletionReason(deleted == null ? null : deletionReason)
				.build();
	}

	private Object mapDocumentItem(Object dictDataItem, DictField field)
	{
		if (field.getType() == DictFieldType.DECIMAL && dictDataItem instanceof Integer value)
		{
			return Decimal128.parse(String.valueOf(value.doubleValue()));
		}

		if (field.getType() == DictFieldType.DECIMAL && dictDataItem instanceof Double value)
		{
			return Decimal128.parse(String.valueOf(value));
		}

		if (field.getType() == DictFieldType.DECIMAL && dictDataItem instanceof BigDecimal value)
		{
			return new Decimal128(value);
		}

		return dictDataItem;
	}

	private Date serviceDateTime(LocalDateTime dateTime)
	{
		return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
	}

	private void setValue(Map<String, DictField> dictFieldMap, Set<String> refFieldIds, Map.Entry<String, Object> entry)
	{
		entry.setValue(refFieldIds.contains(entry.getKey()) && entry.getValue() instanceof Document document
				? mapFrom(dictFieldMap.get(entry.getKey()).getDictRef().getDictId(), document)
				: mapDictDataItem(entry.getValue(), dictFieldMap.get(entry.getKey())));
	}

	private Object mapDictDataItem(Object documentItem, DictField field)
	{
		return field.isMultivalued() ? mapMultipleValue(documentItem, field) : mapSingleValue(documentItem, field);
	}

	private Object mapMultipleValue(Object value, DictField field)
	{
		return value == null ? Collections.emptyList() : ((Collection<?>) value).stream()
				.map(it -> mapSingleValue(it, field))
				.toList();
	}

	/**
	 * Для Decimal поля конвертируем исходный тип в BigDecimal
	 * Для Date/Timestamp поля конвертируем исходный тип в LocalDate/LocalDateTime, в зависимости от типа поля справочника.
	 */
	private Object mapSingleValue(Object value, DictField field)
	{
		var fieldType = field.getType();

		if (value instanceof Decimal128 decimal128)
		{
			return decimal128.bigDecimalValue();
		}

		if (value instanceof Date date)
		{
			return switch (fieldType)
					{
						case DATE -> mapToLocalDate(date);
						case TIMESTAMP -> mapToLocalDateTime(date);
						default -> throw new FieldValidationException("Невозможно привести поле %s к типу %s.".formatted(field.getId(), fieldType.name()));
					};
		}

		if (DictFieldType.JSON.equals(fieldType))
		{
			return Collections.unmodifiableMap((Map<?, ?>) value);
		}

		if (DictFieldType.GEO_JSON.equals(fieldType))
		{
			return value == null ? null : JsonUtils.asTypeReference(value, GeoJsonObject.class);
		}

		return value;
	}

	private LocalDateTime mapToLocalDateTime(Date value)
	{
		return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

	private LocalDate mapToLocalDate(Date value)
	{
		return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}
}
