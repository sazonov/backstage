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

import com.proit.app.dict.configuration.backend.provider.DictSchemeBackendProvider;
import com.proit.app.dict.constant.ServiceFieldConstants;
import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.domain.DictField;
import com.proit.app.dict.exception.EngineException;
import com.proit.app.dict.exception.dict.enums.EnumNotFoundException;
import com.proit.app.dict.exception.dict.field.FieldValidationException;
import com.proit.app.dict.exception.dict.field.ForbiddenFieldNameException;
import com.proit.app.dict.service.DictService;
import com.proit.app.dict.api.domain.DictFieldType;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class DictValidationService
{
	private static final int DICT_FIELD_ID_MAX_LENGTH = 32;

	private final DictService dictService;

	private final DictSchemeBackendProvider schemeBackendProvider;

	public DictValidationService(@Lazy DictService dictService, DictSchemeBackendProvider schemeBackendProvider)
	{
		this.dictService = dictService;
		this.schemeBackendProvider = schemeBackendProvider;
	}

	public void validateDictScheme(Dict dict)
	{
		validateDictEngine(dict);

		validateServiceFields(dict);

		validateFieldType(dict);
	}

	private void validateDictEngine(Dict dict)
	{
		if (dict.getEngine() == null)
		{
			throw new EngineException("Значение engine для справочника '%s' не может быть null.".formatted(dict.getId()));
		}

		schemeBackendProvider.getBackendByEngineName(dict.getEngine().getName());
	}

	private void validateServiceFields(Dict dict)
	{
		dict.getFields()
				.stream()
				.filter(field -> ServiceFieldConstants.getServiceSchemeFields().contains(field.getId()))
				.findAny()
				.ifPresent(it -> {
					throw new ForbiddenFieldNameException(it.getId());
				});
	}

	private void validateFieldType(Dict dict)
	{
		dict.getFields()
				.forEach(field -> validateField(dict, field));
	}

	private void validateField(Dict dict, DictField field)
	{
		if (field.getId().length() > DICT_FIELD_ID_MAX_LENGTH)
		{
			throw new FieldValidationException("Для поля %s превышена максимальная длина идентификатора в %s символов.".formatted(field.getId(), DICT_FIELD_ID_MAX_LENGTH));
		}
		else if (field.getType() == DictFieldType.DICT)
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
