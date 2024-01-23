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

package com.proit.app.dict.service.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proit.app.dict.api.domain.DictFieldType;
import com.proit.app.dict.constant.ServiceFieldConstants;
import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.domain.DictField;
import com.proit.app.dict.domain.DictFieldName;
import com.proit.app.dict.exception.dict.DictConcurrentUpdateException;
import com.proit.app.dict.exception.dict.UnavailableDictRefException;
import com.proit.app.dict.exception.dict.enums.EnumNotFoundException;
import com.proit.app.dict.exception.dict.field.FieldNotFoundException;
import com.proit.app.dict.exception.dict.field.FieldValidationException;
import com.proit.app.dict.exception.dict.field.ForbiddenFieldNameException;
import com.proit.app.dict.model.dictitem.DictDataItem;
import com.proit.app.dict.service.DictDataService;
import com.proit.app.dict.service.DictService;
import com.proit.app.dict.service.mapping.DictFieldNameMappingService;
import com.proit.app.model.other.date.DateConstants;
import org.bson.types.Decimal128;
import org.geojson.GeoJsonObject;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class DictDataValidationService
{
	private final ObjectMapper objectMapper;

	private final DictService dictService;
	private final DictDataService dictDataService;

	private final DictFieldNameMappingService fieldNameMappingService;

	public DictDataValidationService(DictService dictService, @Lazy DictDataService dictDataService, ObjectMapper objectMapper, DictFieldNameMappingService fieldNameMappingService)
	{
		this.dictService = dictService;
		this.dictDataService = dictDataService;
		this.objectMapper = objectMapper;
		this.fieldNameMappingService = fieldNameMappingService;
	}

	public void validateSelectFields(Dict dict, List<DictFieldName> selectFields)
	{
		validateRefDict(dict, selectFields);

		var withoutRefFields = selectFields.stream()
				.filter(it -> it.getDictId() == null)
				.map(DictFieldName::getFieldId)
				.toList();

		validateFieldsBySingleScheme(dict, withoutRefFields);

		selectFields.stream()
				.filter(it -> it.getDictId() != null)
				.collect(Collectors.groupingBy(
						DictFieldName::getDictId, Collectors.mapping(DictFieldName::getFieldId, Collectors.toList()))
				)
				.forEach(this::validateFieldsBySingleScheme);
	}

	public void validatePageable(Dict dict, Pageable pageable)
	{
		if (pageable != null)
		{
			var sortedFields = pageable.getSort()
					.stream()
					.map(Sort.Order::getProperty)
					.map(this::dictFieldName)
					.toList();

			var withoutRefFields = sortedFields.stream()
					.filter(it -> it.getDictId() == null)
					.map(DictFieldName::getFieldId)
					.toList();

			validateFieldsBySingleScheme(dict, withoutRefFields);

			validateRefDict(dict, sortedFields);
		}
	}

	private DictFieldName dictFieldName(String field)
	{
		return fieldNameMappingService.mapDictFieldName(field);
	}

	//TODO: после слияния KGH-4229, актуализировать без проброса userId
	public void validateDictDataItem(DictDataItem dataItem, String userId)
	{
		var dataItemMap = dataItem.getDataItemMap();

		var availableFields = dictService.getById(dataItem.getDictId()).getFields();

		var availableFieldIds = getAvailableFieldIds(availableFields);

		dataItemMap.forEach((field, value) -> checkForbiddenField(availableFieldIds, field));

		validateRequiredFields(dataItem.getDictId(), dataItemMap, availableFields, userId);
	}

	public void validateOptimisticLock(String dictId, String itemId, long version, String userId)
	{
		var current = dictDataService.getById(dictId, itemId, userId);

		if (version != current.getVersion())
		{
			throw new DictConcurrentUpdateException(version, current.getVersion());
		}
	}

	private void validateRequiredFields(String dictId, Map<String, Object> dictData, List<DictField> availableFields, String userId)
	{
		availableFields.stream()
				.filter(field -> !ServiceFieldConstants.getServiceInsertableFields().contains(field.getId()))
				.peek(dictField -> checkCast(dictId, dictField, dictData.get(dictField.getId()), userId))
				.filter(DictField::isRequired)
				.filter(dictField -> !dictData.containsKey(dictField.getId()) || dictData.get(dictField.getId()) == null)
				.findAny()
				.ifPresent(field -> {
					throw new FieldValidationException("Отсутствует обязательное поле: %s.".formatted(field.getId()));
				});
	}

	private void checkCast(String dictId, DictField dictField, Object value, String userId)
	{
		if (value == null)
		{
			return;
		}
		if (value instanceof List<?> list)
		{
			if (dictField.isMultivalued())
			{
				list.forEach(it -> checkSingleElementCast(dictId, dictField, it, userId));
			}
			else
			{
				throw new FieldValidationException("Не может быть массивом: %s.".formatted(dictField.getId()));
			}
		}
		else
		{
			checkSingleElementCast(dictId, dictField, value, userId);
		}
	}

	//TODO: провести декомпозицию метода
	private void checkSingleElementCast(String dictId, DictField dictField, Object value, String userId)
	{
		try
		{
			switch (dictField.getType())
			{
				case INTEGER -> {
					Long number = null;

					if (value instanceof Double d)
					{
						if (d.longValue() != d)
						{
							throw new FieldValidationException("Double вместо Integer: %s.".formatted(dictField.getId()));
						}

						number = d.longValue();
					}

					if (value instanceof Integer i)
					{
						number = i.longValue();
					}

					if (value instanceof Long l)
					{
						number = l;
					}

					Assert.notNull(number, "Недопустимый тип '%s' для значения '%s'".formatted(value.getClass().getTypeName(), value));

					if (checkNumberValue(number, (Integer) dictField.getMinSize(), (Integer) dictField.getMaxSize()))
					{
						throw new FieldValidationException("Превышена допустимые ограничения числа: %s.".formatted(dictField.getId()));
					}
				}
				case DECIMAL -> {
					BigDecimal decimal = null;

					if (value instanceof Integer i)
					{
						decimal = BigDecimal.valueOf(i);
					}

					if (value instanceof Double d)
					{
						decimal = BigDecimal.valueOf(d);
					}

					if (value instanceof BigDecimal bd)
					{
						decimal = bd;
					}

					if (value instanceof Decimal128 d)
					{
						throw new FieldValidationException("Недопустимый тип '%s' для значения '%s'".formatted(Decimal128.class.getTypeName(), d));
					}

					Assert.notNull(decimal, "Неизвестный тип '%s' для поля '%s'".formatted(value.getClass().getTypeName(), dictField.getId()));

					if (checkNumberValue(decimal.doubleValue(), dictField.getMinSize(), dictField.getMaxSize()))
					{
						throw new FieldValidationException("Превышена допустимые ограничения вещественного числа: %s.".formatted(dictField.getId()));
					}
				}
				case BOOLEAN -> Assert.isInstanceOf(Boolean.class, value, "Недопустимый тип '%s' для поля '%s'.".formatted(value.getClass().getTypeName(), dictField.getId()));
				case STRING -> {
					Assert.isInstanceOf(String.class, value, "Недопустимый тип '%s' для поля '%s'.".formatted(value.getClass().getTypeName(), dictField.getId()));

					if (checkStringLength(((String) value).length(), (Integer) dictField.getMinSize(), (Integer) dictField.getMaxSize()))
					{
						throw new FieldValidationException("Превышена допустимая длина строки: %s.".formatted(dictField.getId()));
					}
				}
				case DICT -> {
					var r = dictDataService.getByFilter(dictField.getDictRef().getDictId(), List.of("id"),
							"%s = '%s'".formatted(ServiceFieldConstants.ID, value), Pageable.unpaged(), userId);

					if (r.isEmpty())
					{
						throw new FieldValidationException("Значение отсутствует в связанном справочнике: %s.".formatted(dictField.getId()));
					}
				}
				case DATE -> {
					if (value instanceof Date || value instanceof LocalDate)
					{
						break;
					}

					LocalDate.parse((String) value, DateConstants.ISO_DATE_FORMATTER);
				}
				case TIMESTAMP -> {
					if (value instanceof Date || value instanceof LocalDateTime)
					{
						break;
					}

					LocalDateTime.parse((String) value, DateConstants.ISO_OFFSET_DATE_TIME_MS_FORMATTER);
				}
				case JSON -> {
					if (value instanceof String s)
					{
						try
						{
							objectMapper.readValue(s, Map.class);
						}
						catch (JsonProcessingException ex)
						{
							throw new FieldValidationException("Некорретный формат json поля: %s.".formatted(dictField.getId()));
						}
					}
					else
					{
						var o = (Map<?, ?>) value;
					}
				}
				case ENUM -> {

					var contains = dictService.getById(dictId)
							.getEnums()
							.stream()
							.filter(it -> it.getId().equals(dictField.getEnumId()))
							.findAny()
							.orElseThrow(() -> new EnumNotFoundException(dictField.getEnumId()))
							.getValues()
							.contains((String) value);

					if (!contains)
					{
						throw new FieldValidationException("Неизвестное значение enum: %s.".formatted(dictField.getId()));
					}
				}
				case GEO_JSON -> {
					//TODO: реализовать персистентное хранение значений GEO_JSON как GeoJson обьектов (postgis)
					if (value instanceof String s)
					{
						objectMapper.readValue(s, GeoJsonObject.class);

						return;
					}

					throw new FieldValidationException("Недопустимый тип '%s' значения для поля: %s".formatted(value.getClass().getTypeName(), dictField.getId()));
				}
				case ATTACHMENT -> {
					var s = (String) value;
				}
				default -> throw new FieldValidationException("Неизвестный тип данных: %s.".formatted(dictField.getId()));
			}
		}
		catch (Exception e)
		{
			throw new FieldValidationException("Некорректный формат данных: %s.".formatted(dictField.getId()), e);
		}
	}

	private boolean checkStringLength(int length, Integer min, Integer max)
	{
		return (max != null && length > max) || (min != null && length < min);
	}

	private boolean checkNumberValue(Double value, Number min, Number max)
	{
		return (max != null && value > max.doubleValue()) || (min != null && value < min.doubleValue());
	}

	private boolean checkNumberValue(Long value, Integer min, Integer max)
	{
		return (max != null && value > max) || (min != null && value < min);
	}

	private void checkForbiddenField(Set<String> availableFieldIds, String fieldId)
	{
		if (ServiceFieldConstants.getServiceInsertableFields().contains(fieldId))
		{
			throw new ForbiddenFieldNameException(fieldId);
		}

		if (!availableFieldIds.contains(fieldId))
		{
			throw new ForbiddenFieldNameException(fieldId);
		}
	}

	private void validateRefDict(Dict scheme, List<DictFieldName> selectFields)
	{
		var availableRefDicts = scheme.getFields()
				.stream()
				.filter(it -> it.getType() == DictFieldType.DICT)
				.map(it -> it.getDictRef().getDictId())
				.map(dictService::getById)
				.collect(Collectors.toMap(Dict::getId, Function.identity()));

		selectFields.stream()
				.filter(it -> it.getDictId() != null)
				.filter(it -> !availableRefDicts.containsKey(it.getDictId()))
				.findAny()
				.ifPresent(it -> {
					throw new UnavailableDictRefException(it.getDictId());
				});

		var refFieldIds = selectFields.stream()
				.filter(it -> it.getDictId() != null)
				.map(DictFieldName::getFieldId)
				.filter(fieldId -> !"*".equals(fieldId))
				.collect(Collectors.toList());

		selectFields.stream()
				.filter(it -> it.getDictId() != null)
				.filter(it -> !"*".equals(it.getFieldId()))
				.map(it -> availableRefDicts.get(it.getDictId()))
				.forEach(it -> validateFieldsBySingleScheme(it, refFieldIds));
	}

	private void validateFieldsBySingleScheme(String dictId, List<String> selectFields)
	{
		var dict = dictService.getById(dictId);

		validateFieldsBySingleScheme(dict, selectFields);
	}

	private void validateFieldsBySingleScheme(Dict dict, List<String> selectFields)
	{
		var availableFieldIds = getAvailableFieldIds(dict.getFields());

		selectFields.stream()
				.filter(it -> !"*".equals(it))
				.filter(Predicate.not(availableFieldIds::contains))
				.findAny()
				.ifPresent(fieldId -> {
					throw new FieldNotFoundException(dict.getId(), fieldId);
				});
	}

	private Set<String> getAvailableFieldIds(List<DictField> fields)
	{
		return fields.stream()
				.map(DictField::getId)
				.collect(Collectors.toSet());
	}
}
