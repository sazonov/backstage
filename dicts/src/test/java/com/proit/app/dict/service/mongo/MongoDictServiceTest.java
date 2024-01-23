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

import com.proit.app.dict.common.TestPipeline;
import com.proit.app.dict.domain.DictEngine;
import com.proit.app.dict.exception.dict.DictNotFoundException;
import com.proit.app.dict.service.CommonDictServiceTest;
import com.proit.app.dict.service.backend.postgres.PostgresEngine;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Order(TestPipeline.MONGO_DICT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@MongoStorage
public class MongoDictServiceTest extends CommonDictServiceTest
{
	@Test
	void getById_DictDeleted()
	{
		getByIdDeletedDict(MONGO_DICT_ID);
	}

	@Test
	void getById_DictNotFound()
	{
		assertThrows(DictNotFoundException.class, () -> dictService.getById("incorrect"));
	}

	@Test
	void create()
	{
		createDict(MONGO_DICT_ID);
	}

	@Test
	void create_AlreadyExists()
	{
		createAlreadyExistsDict(MONGO_DICT_ID);
	}

	@Test
	void create_withUUIDDictFieldIds()
	{
		createWithUUIDDictFieldIds();
	}

	@Test
	void delete()
	{
		deleteDict(MONGO_DICT_ID);
	}

	@Test
	void deleteWithAnotherEngine()
	{
		deleteDictWithAnotherEngine(MONGO_DICT_ID, new DictEngine(PostgresEngine.POSTGRES));
	}

	@Test
	void update()
	{
		updateDict(MONGO_DICT_ID);
	}

	@Test
	void update_withRenameDictField()
	{
		renameDictField(MONGO_DICT_ID);
	}

	@Test
	void createIndex()
	{
		createDictIndex(MONGO_DICT_ID);
	}

	@Test
	void deleteIndex()
	{
		deleteDictIndex(MONGO_DICT_ID);
	}

	@Test
	void createConstraint()
	{
		createDictConstraint(MONGO_DICT_ID);
	}

	@Test
	void deleteConstraint()
	{
		deleteDictConstraint(MONGO_DICT_ID);
	}

	@Test
	void createEnum()
	{
		createDictEnum(MONGO_DICT_ID);
	}

	@Test
	void deleteEnum()
	{
		deleteDictEnum(MONGO_DICT_ID);
	}

	@Test
	void updateEnum()
	{
		updateDictEnum(MONGO_DICT_ID);
	}

	@Test
	@Order(TestPipeline.DICT_GET_BY_ID_TEST)
	void getById()
	{
		getDictById(MONGO_DICT_ID);
	}

	@Test
	@Order(TestPipeline.DICT_GET_ALL_TEST)
	void getAll()
	{
		assertEquals(49, dictService.getAll().size());
	}
}
