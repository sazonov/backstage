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
import com.proit.app.domain.Dict;
import com.proit.app.domain.DictField;
import com.proit.app.domain.DictFieldName;
import com.proit.app.domain.DictFieldType;
import com.proit.app.exception.*;
import com.proit.app.service.backend.DictBackend;
import lombok.RequiredArgsConstructor;
import org.bson.types.Decimal128;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.proit.app.constant.ServiceFieldConstants.ID;
import static com.proit.app.constant.ServiceFieldConstants._ID;

@Service
@RequiredArgsConstructor
public class ValidationService
{
	private final DictBackend dictBackend;

	public void validateScheme(Dict dict)
	{
		dict.getFields()
				.stream()
				.filter(field -> ServiceFieldConstants.getServiceSchemeFields().contains(field.getId()))
				.findAny()
				.ifPresent(it -> {
					throw new ForbiddenFieldNameException(it.getId());
				});

		dict.getFields().forEach(field -> {
			if (field.getType() == DictFieldType.DICT)
			{
				try
				{
					dictBackend.getDictById(field.getDictRef().getDictId());
				}
				catch (Exception e)
				{
					throw new FieldValidationException("Ошибка валидации поля-референса: %s.".formatted(field.getId()), e);
				}
			}
			else if (field.getType() == DictFieldType.ENUM)
			{
				dict.getEnums()
						.stream()
						.filter(it -> it.getId().equals(field.getEnumId()))
						.findAny()
						.orElseThrow(() -> new EnumNotFoundException(field.getEnumId()));
			}
			else if (field.getType() == DictFieldType.DECIMAL)
			{
				if (!checkDecimalMaxMinCondition(field))
				{
					throw new FieldValidationException("Значение minSize и maxSize для поля %s может быть только INTEGER или DECIMAL.".formatted(field.getId()));
				}
			}
			else if (field.getType() == DictFieldType.INTEGER || field.getType() == DictFieldType.STRING)
			{
				if (!checkIntegerMaxMinCondition(field))
				{
					throw new FieldValidationException("Значение minSize и maxSize для поля %s может быть только INTEGER.".formatted(field.getId()));
				}
			}
		});
	}

	public void validateSelectFields(String dictId, List<DictFieldName> selectFields)
	{
		var scheme = dictBackend.getDictById(dictId);

		validateJoins(scheme, selectFields);

		validateFieldsBySingleScheme(scheme, selectFields.stream()
				.filter(it -> it.getDictId() == null)
				.map(DictFieldName::getFieldId)
				.toList()
		);

		Map<String, List<String>> refFields = new HashMap<>();

		selectFields.stream()
				.filter(it -> it.getDictId() != null)
				.forEach(it -> {
					refFields.putIfAbsent(it.getDictId(), new ArrayList<>());
					refFields.get(it.getDictId())
							.add(it.getFieldId());
				});

		refFields.forEach(this::validateFieldsBySingleScheme);
	}

	public void validateDocInsert(String dictId, Map<String, Object> doc)
	{
		var availableFields = dictBackend.getDictById(dictId)
				.getFields();

		var availableFieldIds = availableFields
				.stream()
				.map(DictField::getId)
				.map(it -> it.equals(_ID) ? ID : it)
				.collect(Collectors.toSet());

		doc.forEach((key, value) -> checkForbiddenField(availableFieldIds, key));

		availableFields.stream()
				.filter(field -> !ServiceFieldConstants.getServiceInsertableFields().contains(field.getId()))
				.peek(dictField -> checkCast(dictId, dictField, doc.get(dictField.getId())))
				.filter(DictField::isRequired)
				.filter(dictField -> !doc.containsKey(dictField.getId()))
				.findAny()
				.ifPresent(field -> {
					throw new FieldValidationException("Отсутствует обязательное поле: %s.".formatted(field.getId()));
				});
	}

	private void checkCast(String dictId, DictField dictField, Object value)
	{
		if (value == null)
		{
			return;
		}
		if (value instanceof List<?> list)
		{
			if (dictField.isMultivalued())
			{
				list.forEach(it -> checkSingleElementCast(dictId, dictField, it));
			}
			else
			{
				throw new FieldValidationException("Не может быть массивом: %s.".formatted(dictField.getId()));
			}
		}
		else
		{
			checkSingleElementCast(dictId, dictField, value);
		}
	}

	private void checkSingleElementCast(String dictId, DictField dictField, Object value)
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
					var r = dictBackend.getByFilter(
							dictBackend.getDictById(dictField.getDictRef().getDictId()),
							List.of(new DictFieldName(null, "id")),
							"%s = '%s'".formatted(ID, value), Pageable.unpaged());

					if (r.isEmpty())
					{
						throw new FieldValidationException("Значение отсутствует в связанном справочнике: %s.".formatted(dictField.getId()));
					}
				}
				case DATE -> LocalDate.parse((String) value, MappingService.DATE_FORMATTER);
				case TIMESTAMP -> LocalDateTime.parse((String) value, MappingService.DATE_TIME_FORMATTER);
				case JSON -> {
					var o = (Map<?, ?>) value;
				}
				case ENUM -> {

					var contains = dictBackend.getDictById(dictId)
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
	private boolean checkDecimalMaxMinCondition(DictField field)
	{
		return (checkSingleElementType(field.getMinSize(), Integer.class) || checkSingleElementType(field.getMinSize(), Double.class))
				&& (checkSingleElementType(field.getMaxSize(), Integer.class) || checkSingleElementType(field.getMaxSize(), Double.class));
	}

	private boolean checkIntegerMaxMinCondition(DictField field)
	{
		return checkSingleElementType(field.getMinSize(), Integer.class) && checkSingleElementType(field.getMaxSize(), Integer.class);
	}

	private boolean checkSingleElementType(Object value, Class<?> clazz)
	{
		return value == null || value.getClass().equals(clazz);
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
		var dict = dictBackend.getDictById(dictId);

		validateFieldsBySingleScheme(dict, selectFields);
	}

	private void validateFieldsBySingleScheme(Dict dict, List<String> selectFields)
	{
		var availableFields = dict.getFields()
				.stream()
				.map(DictField::getId)
				.collect(Collectors.toSet());

		selectFields.stream()
				.filter(it -> !it.equals("*"))
				.forEach(field -> {
					if (!availableFields.contains(field))
					{
						throw new FieldNotFoundException(dict.getId(), field);
					}
				});
	}
}
