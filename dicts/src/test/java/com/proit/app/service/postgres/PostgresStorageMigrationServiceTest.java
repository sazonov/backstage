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

package com.proit.app.service.postgres;

import com.proit.app.common.CommonTest;
import com.proit.app.common.TestPipeline;
import com.proit.app.domain.Dict;
import com.proit.app.domain.DictEngine;
import com.proit.app.service.backend.postgres.PostgresDictBackend;
import com.proit.app.service.backend.postgres.PostgresDictTransactionBackend;
import com.proit.app.service.backend.postgres.PostgresEngine;
import com.proit.app.service.backend.postgres.PostgresVersionSchemeBackend;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Order(TestPipeline.POSTGRES_STORAGE_MIGRATION)
@PostgresStorage
public class PostgresStorageMigrationServiceTest extends CommonTest
{
	public static final String POSTGRES_MIGRATION_DICT_ID = "postgresMigrate";

	@Test
	void postgresStorageMigration()
	{
		assertEquals(dictBackend.getClass(), PostgresDictBackend.class);
		assertEquals(versionSchemeBackend.getClass(), PostgresVersionSchemeBackend.class);
		assertEquals(dictTransactionBackend.getClass(), PostgresDictTransactionBackend.class);

		assertEquals(PostgresEngine.POSTGRES, dictsProperties.getStorage());

		var correctDictsEngine = dictService.getAll()
				.stream()
				.filter(it -> it.getId().startsWith(POSTGRES_MIGRATION_DICT_ID))
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
