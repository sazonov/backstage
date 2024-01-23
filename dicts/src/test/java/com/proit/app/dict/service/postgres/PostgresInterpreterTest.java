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
import com.proit.app.dict.service.CommonInterpreterTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

@Order(TestPipeline.POSTGRES_INTERPRETER)
@PostgresStorage
public class PostgresInterpreterTest extends CommonInterpreterTest
{
	@BeforeAll
	public void buildTestableDictHierarchy()
	{
		buildTestableDictHierarchy(POSTGRES_DICT_ID);
	}

	@Test
	protected void update_ByColumnValue()
	{
		updateByColumnValue(POSTGRES_DICT_ID);
	}

	@Test
	protected void parse_UpdatePositiveCondition()
	{
		parseUpdatePositiveCondition();
	}

	@Test
	protected void parse_UpdateNegativeCondition()
	{
		parseUpdateNegativeCondition();
	}

	@Test
	protected void parse_UpdateWithoutCondition()
	{
		parseUpdateWithoutCondition();
	}

	@Test
	protected void parse_DeleteNegativeCondition()
	{
		parseDeleteNegativeCondition();
	}

	@Test
	protected void parse_DeleteWithoutCondition()
	{
		parseDeleteWithoutCondition();
	}
}
