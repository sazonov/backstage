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

package com.proit.app.dict.service.postgres;

import com.proit.app.dict.common.TestPipeline;
import com.proit.app.dict.service.CommonDictValidationServiceTest;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

@Order(TestPipeline.POSTGRES_DICT_VALIDATION)
@PostgresStorage
public class PostgresDictValidationServiceTest extends CommonDictValidationServiceTest
{
	@Test
	void validate_SchemeCorrect()
	{
		validateScheme();
	}

	@Test
	void validate_SchemeForbiddenField()
	{
		validateSchemeForbiddenField();
	}

	@Test
	void validate_SchemeIncorrectFieldsIdMaxLength()
	{
		validateSchemaIncorrectFieldsIdMaxLength();
	}

	@Test
	void validate_SchemeEngineNotNull()
	{
		validateSchemeEngineNotNull();
	}
}
