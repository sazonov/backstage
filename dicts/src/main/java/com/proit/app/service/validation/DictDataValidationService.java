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

package com.proit.app.service.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proit.app.constant.ServiceFieldConstants;
import com.proit.app.domain.Dict;
import com.proit.app.domain.DictField;
import com.proit.app.domain.DictFieldName;
import com.proit.app.domain.DictFieldType;
import com.proit.app.exception.dictionary.UnavailableDictRefException;
import com.proit.app.exception.dictionary.enums.EnumNotFoundException;
import com.proit.app.exception.dictionary.field.FieldNotFoundException;
import com.proit.app.exception.dictionary.field.FieldValidationException;
import com.proit.app.exception.dictionary.field.ForbiddenFieldNameException;
import com.proit.app.model.dictitem.DictDataItem;
import com.proit.app.model.other.date.DateConstants;
import com.proit.app.service.DictDataService;
import com.proit.app.service.DictService;
import org.bson.types.Decimal128;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.proit.app.constant.ServiceFieldConstants.ID;

@Service
public class DictDataValidationService
{
	private final ObjectMapper objectMapper;

	private final DictService dictService;
	private final DictDataService dictDataService;

	public DictDataValidationService(DictService dictService, @Lazy DictDataService dictDataService, ObjectMapper objectMapper)
	{
		this.dictService = dictService;
		this.dictDataService = dictDataService;
		this.objectMapper = objectMapper;
	}

	public void validateSelectFields(String dictId, List<DictFieldName> selectFields)
	{
		var dict = dictService.getById(dictId);

		validateJoins(dict, selectFields);

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

	//TODO: после слияния KGH-4229, актуализировать без проброса userId
	public void validateDictDataItem(DictDataItem dataItem, String userId)
	{
		var dataItemMap = dataItem.getDataItemMap();

		var availableFields = dictService.getById(dataItem.getDictId()).getFields();

		var availableFieldIds = getAvailableFieldIds(availableFields);

		dataItemMap.forEach((field, value) -> checkForbiddenField(availableFieldIds, field));

		validateRequiredFields(dataItem.getDictId(), dataItemMap, availableFields, userId);
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

	private void checkSingleElementCast(String dictId, DictField dictField, Object value, String userId)
	{
		try
		{
			switch (dictField.getType())
			{
				case INTEGER -> {
					if (value instanceof Integer || value instanceof Long)
					{
						var castedValue = value instanceof Integer
								? ((Integer) value).longValue()
								: (Long) value;

						if (checkNumberValue(castedValue, (Integer) dictField.getMinSize(), (Integer) dictField.getMaxSize()))
						{
							throw new FieldValidationException("Превышена допустимые ограничения числа: %s.".formatted(dictField.getId()));
						}

						break;
					}

					var d = (Double) value;

					if (d.intValue() != d)
					{
						throw new FieldValidationException("Double вместо Integer: %s.".formatted(dictField.getId()));
					}
				}
				case DECIMAL -> {
					//todo: KGH-3049 рефакторинг, декомпозиция метода

					Decimal128 decimal;

					if (value instanceof Integer || value instanceof Double)
					{
						var d = value instanceof Integer
								? ((Integer) value).doubleValue()
								: (Double) value;

						decimal = Decimal128.parse(String.valueOf(d));
					}
					else if (value instanceof BigDecimal)
					{
						decimal = new Decimal128((BigDecimal) value);
					}
					else
					{
						decimal = (Decimal128) value;
					}

					if (checkNumberValue(decimal.doubleValue(), dictField.getMinSize(), dictField.getMaxSize()))
					{
						throw new FieldValidationException("Превышена допустимые ограничения вещественного числа: %s.".formatted(dictField.getId()));
					}
				}
				case BOOLEAN -> {
					var b = (Boolean) value;
				}
				case STRING -> {
					if (checkStringLength(((String) value).length(), (Integer) dictField.getMinSize(), (Integer) dictField.getMaxSize()))
					{
						throw new FieldValidationException("Превышена допустимая длина строки: %s.".formatted(dictField.getId()));
					}
				}
				case DICT -> {
					var r = dictDataService.getByFilter(dictField.getDictRef().getDictId(), List.of("id"),
							"%s = '%s'".formatted(ID, value), Pageable.unpaged(), userId);

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

					LocalDate.parse((String) value, DateConstants.DATE_FORMATTER);
				}
				case TIMESTAMP -> {
					if (value instanceof Date || value instanceof LocalDateTime)
					{
						break;
					}

					LocalDateTime.parse((String) value, DateConstants.DATE_TIME_FORMATTER);
				}
				case JSON -> {
					if (value instanceof String s)
					{
						try
						{
							var map = objectMapper.readValue(s, Map.class);
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
				//todo: KGH-4512 реализовать валидацию GEO_JSON поля
				case ATTACHMENT, GEO_JSON -> {
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

	private void validateJoins(Dict scheme, List<DictFieldName> selectFields)
	{
		var availableJoins = scheme.getFields()
				.stream()
				.filter(it -> it.getType() == DictFieldType.DICT)
				.map(it -> it.getDictRef().getDictId())
				.toList();

		selectFields.stream()
				.filter(it -> it.getDictId() != null)
				.filter(it -> !availableJoins.contains(it.getDictId()))
				.findAny()
				.ifPresent(it -> {
					throw new UnavailableDictRefException(it.getDictId());
				});
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
				.filter(it -> !it.equals("*"))
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
