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

import com.proit.app.constant.ServiceFieldConstants;
import com.proit.app.domain.Dict;
import com.proit.app.domain.DictField;
import com.proit.app.domain.DictFieldType;
import com.proit.app.exception.EnumNotFoundException;
import com.proit.app.exception.FieldValidationException;
import com.proit.app.exception.ForbiddenFieldNameException;
import com.proit.app.service.DictService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class DictValidationServiceImpl implements DictValidationService
{
	private final DictService dictService;

	public DictValidationServiceImpl(@Lazy DictService dictService)
	{
		this.dictService = dictService;
	}

	@Override
	public void validateDictScheme(Dict dict)
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
					dictService.getById(field.getDictRef().getDictId());
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
}
