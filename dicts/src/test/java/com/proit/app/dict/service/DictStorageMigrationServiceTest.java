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
import com.proit.app.dict.domain.DictEngine;
import com.proit.app.dict.exception.dict.DictStorageMigrationException;
import com.proit.app.dict.service.backend.mongo.MongoEngine;
import com.proit.app.dict.service.backend.postgres.PostgresEngine;
import com.proit.app.dict.service.migration.DictStorageMigrationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DictStorageMigrationServiceTest extends CommonTest
{
	protected String MONGO_STORAGE_DICT_ID;
	protected String POSTGRES_STORAGE_DICT_ID;

	@Autowired private DictService dictService;

	@Autowired private DictDataService dictDataService;

	@Autowired private DictStorageMigrationService dictStorageMigrationService;

	@BeforeAll
	public void createTestableHierarchy()
	{
		MONGO_STORAGE_DICT_ID = createNewDict("mongo", new DictEngine(MongoEngine.MONGO)).getId();
		POSTGRES_STORAGE_DICT_ID = createNewDict("postgres", new DictEngine(PostgresEngine.POSTGRES)).getId();

		addDictData(MONGO_STORAGE_DICT_ID);
		addDictData(MONGO_STORAGE_DICT_ID);
		addDictData(POSTGRES_STORAGE_DICT_ID);
		addDictData(POSTGRES_STORAGE_DICT_ID);
	}

	@Test
	public void migratePostgresStorageDictToPostgresStorageUncorrected()
	{
		var sourceDict = dictService.getById(POSTGRES_STORAGE_DICT_ID);
		var sourceDictEngine = sourceDict.getEngine();

		assertThrows(DictStorageMigrationException.class, () -> dictStorageMigrationService.migrate(sourceDict, sourceDictEngine, sourceDictEngine));
	}

	@Test
	public void migrateNullableDictUncorrected()
	{
		assertThrows(NullPointerException.class, () -> dictStorageMigrationService.migrate(null, new DictEngine(MongoEngine.MONGO), new DictEngine(PostgresEngine.POSTGRES)));
	}

	@Test
	public void migratePostgresStorageDictToMongoStorageCorrect()
	{
		var sourceDict = dictService.getById(POSTGRES_STORAGE_DICT_ID);
		var sourceDictEngine = sourceDict.getEngine();
		var targetDictEngine = new DictEngine(MongoEngine.MONGO);

		assertEquals(sourceDictEngine.getName(), PostgresEngine.POSTGRES);

		var sourceDictItems = dictDataService.getByFilter(POSTGRES_STORAGE_DICT_ID, List.of("*"), null, Pageable.unpaged())
				.getContent();

		//Миграция справочника из Postgres в Mongo
		sourceDict.setEngine(targetDictEngine);
		sourceDict.setFields(new ArrayList<>(withoutServiceFields(sourceDict.getFields())));

		var migratedToTargetStorageDict = dictService.update(POSTGRES_STORAGE_DICT_ID, sourceDict);
		var migratedToTargetStorageDictItems = dictDataService.getByFilter(POSTGRES_STORAGE_DICT_ID, List.of("*"), null, Pageable.unpaged())
				.getContent();

		assertEquals(migratedToTargetStorageDict.getEngine().getName(), MongoEngine.MONGO);
		assertEquals(sourceDict.getFields().size(), migratedToTargetStorageDict.getFields().size());
		assertEquals(sourceDict.getConstraints().size(), migratedToTargetStorageDict.getConstraints().size());
		assertEquals(sourceDict.getIndexes().size(), migratedToTargetStorageDict.getIndexes().size());
		assertEquals(sourceDictItems.size(), migratedToTargetStorageDictItems.size());
		assertEquals(sourceDictItems.get(0).getData().get("stringField"), migratedToTargetStorageDictItems.get(0).getData().get("stringField"));
		assertEquals(sourceDictItems.get(1).getData().get("integerField"), migratedToTargetStorageDictItems.get(1).getData().get("integerField"));

		//Миграция справочника из Mongo в Postgres
		migratedToTargetStorageDict.setEngine(sourceDictEngine);
		migratedToTargetStorageDict.setFields(new ArrayList<>(withoutServiceFields(migratedToTargetStorageDict.getFields())));

		var migratedToSourceStorageDict = dictService.update(POSTGRES_STORAGE_DICT_ID, migratedToTargetStorageDict);
		var migratedToSourceStorageDictItems = dictDataService.getByFilter(POSTGRES_STORAGE_DICT_ID, List.of("*"), null, Pageable.unpaged())
				.getContent();

		assertEquals(migratedToSourceStorageDict.getEngine().getName(), PostgresEngine.POSTGRES);
		assertEquals(sourceDict.getFields().size(), migratedToSourceStorageDict.getFields().size());
		assertEquals(sourceDict.getConstraints().size(), migratedToSourceStorageDict.getConstraints().size());
		assertEquals(sourceDict.getIndexes().size(), migratedToSourceStorageDict.getIndexes().size());
		assertEquals(sourceDictItems.size(), migratedToSourceStorageDictItems.size());
		assertEquals(sourceDictItems.get(0).getData().get("stringField"), migratedToSourceStorageDictItems.get(0).getData().get("stringField"));
		assertEquals(sourceDictItems.get(1).getData().get("integerField"), migratedToSourceStorageDictItems.get(1).getData().get("integerField"));
	}
}
