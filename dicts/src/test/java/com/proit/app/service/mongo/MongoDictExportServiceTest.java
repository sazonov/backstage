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

package com.proit.app.service.mongo;

import com.proit.app.common.TestPipeline;
import com.proit.app.service.CommonDictExportServiceTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

@Order(TestPipeline.MONGO_EXPORT)
@MongoStorage
public class MongoDictExportServiceTest extends CommonDictExportServiceTest
{
	@BeforeAll
	public void buildTestableDictHierarchy()
	{
		buildTestableDictHierarchy(MONGO_DICT_ID);
	}

	@Test
	void export_jsonNotNullItemsIds()
	{
		exportJsonNotNullItemsIds();
	}

	@Test
	void export_jsonNullItemsIds()
	{
		exportJsonNullItemsIds();
	}

	@Test
	void export_jsonNullDictIdNullItemsIds()
	{
		exportJsonNullDictIdNullItemsIds();
	}

	@Test
	void export_sqlNotNullItemsIds()
	{
		exportSqlNotNullItemsIds();
	}

	@Test
	void export_sqlNullItemsIds()
	{
		exportSqlNullItemsIds();
	}

	@Test
	void export_cvsNotNullItemsIds()
	{
		exportCvsNotNullItemsIds();
	}

	@Test
	void export_cvsNullItemsIds()
	{
		exportCvsNullItemsIds();
	}
}
