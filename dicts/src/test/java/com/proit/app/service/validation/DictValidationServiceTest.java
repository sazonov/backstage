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

import com.proit.app.common.AbstractValidationServiceTest;
import com.proit.app.exception.dictionary.field.ForbiddenFieldNameException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class DictValidationServiceTest extends AbstractValidationServiceTest
{
	@Test
	void validateSchemeCorrect()
	{
		DICT_SCHEME.setFields(FIELDS);

		dictValidationService.validateDictScheme(DICT_SCHEME);
	}

	@Test
	void validateSchemeForbiddenField()
	{
		DICT_SCHEME.setFields(INCORRECT_FIELDS);

		assertThrows(ForbiddenFieldNameException.class, () -> dictValidationService.validateDictScheme(DICT_SCHEME));
	}
}
