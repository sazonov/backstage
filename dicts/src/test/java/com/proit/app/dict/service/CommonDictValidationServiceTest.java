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

package com.proit.app.dict.service;

import com.proit.app.dict.common.CommonTest;
import com.proit.app.dict.configuration.properties.DictsProperties;
import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.domain.DictEngine;
import com.proit.app.dict.domain.DictField;
import com.proit.app.dict.api.domain.DictFieldType;
import com.proit.app.dict.exception.EngineException;
import com.proit.app.dict.exception.dict.field.FieldValidationException;
import com.proit.app.dict.exception.dict.field.ForbiddenFieldNameException;
import com.proit.app.dict.service.validation.DictValidationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommonDictValidationServiceTest extends CommonTest
{
	protected static final List<DictField> FIELDS = new ArrayList<>();
	protected static final List<DictField> FORBIDDEN_FIELDS = new ArrayList<>();
	protected static final List<DictField> INCORRECT_FIELDS_ID_MAX_LENGTH = new ArrayList<>();

	protected static final Dict DICT_SCHEME = Dict.builder()
			.id("test1")
			.name("тест")
			.engine(new DictEngine(DictsProperties.DEFAULT_ENGINE))
			.build();

	@Autowired
	protected DictValidationService dictValidationService;

	@BeforeAll
	public void initValidatableField()
	{
		FIELDS.add(
				DictField.builder()
						.id("stringField")
						.name("строка")
						.type(DictFieldType.STRING)
						.required(true)
						.multivalued(false)
						.build()
		);

		FIELDS.add(
				DictField.builder()
						.id("integerField")
						.name("число")
						.type(DictFieldType.INTEGER)
						.required(true)
						.multivalued(false)
						.build()
		);

		FIELDS.add(
				DictField.builder()
						.id("timestampField")
						.name("Дата и время")
						.type(DictFieldType.TIMESTAMP)
						.required(true)
						.multivalued(false)
						.build()
		);

		FORBIDDEN_FIELDS.add(DictField.builder()
				.id("created")
				.name("Дата и время")
				.type(DictFieldType.TIMESTAMP)
				.required(true)
				.multivalued(false)
				.build());

		INCORRECT_FIELDS_ID_MAX_LENGTH.add(DictField.builder()
				.id(UUID.randomUUID().toString())
				.name("Дата и время")
				.type(DictFieldType.TIMESTAMP)
				.required(true)
				.multivalued(false)
				.build());
	}

	protected void validateScheme()
	{
		DICT_SCHEME.setFields(FIELDS);

		dictValidationService.validateDictScheme(DICT_SCHEME);
	}

	protected void validateSchemeForbiddenField()
	{
		DICT_SCHEME.setFields(FORBIDDEN_FIELDS);

		assertThrows(ForbiddenFieldNameException.class, () -> dictValidationService.validateDictScheme(DICT_SCHEME));
	}

	protected void validateSchemaIncorrectFieldsIdMaxLength()
	{
		DICT_SCHEME.setFields(INCORRECT_FIELDS_ID_MAX_LENGTH);

		assertThrows(FieldValidationException.class, () -> dictValidationService.validateDictScheme(DICT_SCHEME));
	}

	protected void validateSchemeEngineNotNull()
	{
		DICT_SCHEME.setFields(FIELDS);
		DICT_SCHEME.setEngine(null);

		assertThrows(EngineException.class, () -> dictValidationService.validateDictScheme(DICT_SCHEME));

		DICT_SCHEME.setEngine(new DictEngine(DictsProperties.DEFAULT_ENGINE));
	}
}
