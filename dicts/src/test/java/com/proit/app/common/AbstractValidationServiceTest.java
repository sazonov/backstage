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

package com.proit.app.common;

import com.proit.app.domain.Dict;
import com.proit.app.domain.DictField;
import com.proit.app.domain.DictFieldType;
import com.proit.app.service.validation.DictDataValidationService;
import com.proit.app.service.validation.DictValidationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class AbstractValidationServiceTest extends AbstractTest
{
	protected static final List<DictField> fields = new ArrayList<>();
	protected static final List<DictField> incorrectFields = new ArrayList<>();

	protected static final Dict dictScheme = Dict.builder()
			.id("test1")
			.name("тест")
			.build();

	@Autowired
	protected DictValidationService dictValidationService;
	@Autowired
	protected DictDataValidationService dictDataValidationService;

	public AbstractValidationServiceTest()
	{
		fields.add(
				DictField.builder()
						.id("stringField")
						.name("строка")
						.type(DictFieldType.STRING)
						.required(true)
						.multivalued(false)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("integerField")
						.name("число")
						.type(DictFieldType.INTEGER)
						.required(true)
						.multivalued(false)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("timestampField")
						.name("Дата и время")
						.type(DictFieldType.TIMESTAMP)
						.required(true)
						.multivalued(false)
						.build()
		);

		incorrectFields.add(DictField.builder()
				.id("created")
				.name("Дата и время")
				.type(DictFieldType.TIMESTAMP)
				.required(true)
				.multivalued(false)
				.build());
	}
}
