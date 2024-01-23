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

package com.proit.app.dict.service.mongo;

import com.proit.app.dict.common.CommonTest;
import com.proit.app.dict.common.TestPipeline;
import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.domain.DictEngine;
import com.proit.app.dict.service.backend.mongo.MongoDictBackend;
import com.proit.app.dict.service.backend.mongo.MongoDictTransactionBackend;
import com.proit.app.dict.service.backend.mongo.MongoEngine;
import com.proit.app.dict.service.backend.mongo.MongoVersionSchemeBackend;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Order(TestPipeline.MONGO_STORAGE_MIGRATION)
@MongoStorage
public class MongoStorageMigrationServiceTest extends CommonTest
{
	public static final String MONGO_MIGRATION_DICT_ID = "mongoMigrate";

	@Test
	void mongoStorageMigration()
	{
		assertEquals(dictBackend.getClass(), MongoDictBackend.class);
		assertEquals(versionSchemeBackend.getClass(), MongoVersionSchemeBackend.class);
		assertEquals(dictTransactionBackend.getClass(), MongoDictTransactionBackend.class);

		assertEquals(MongoEngine.MONGO, dictsProperties.getStorage());

		var correctDictsEngine = dictService.getAll()
				.stream()
				.filter(it -> it.getId().startsWith(MONGO_MIGRATION_DICT_ID))
				.map(Dict::getEngine)
				.map(DictEngine::getName)
				.allMatch(dictsProperties.getStorage()::equals);

		assertTrue(correctDictsEngine);

		var existsDict = schemeBackendProvider.getBackendByEngineName(dictsProperties.getStorage())
				.getEngine()
				.dictExists();

		var existsVersionScheme = schemeBackendProvider.getBackendByEngineName(dictsProperties.getStorage())
				.getEngine()
				.versionSchemeExists();

		assertTrue(existsDict);
		assertTrue(existsVersionScheme);
	}
}
