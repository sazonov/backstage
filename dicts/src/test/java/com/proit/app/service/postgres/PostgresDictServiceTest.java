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

import com.proit.app.common.TestPipeline;
import com.proit.app.domain.Dict;
import com.proit.app.exception.dict.DictNotFoundException;
import com.proit.app.service.CommonDictServiceTest;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.postgresql.util.PSQLException;
import org.springframework.core.Ordered;

import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Order(TestPipeline.POSTGRES_DICT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@PostgresStorage
public class PostgresDictServiceTest extends CommonDictServiceTest
{
	@Test
	void getById_DictDeleted()
	{
		getByIdDeletedDict(POSTGRES_DICT_ID);
	}

	@Test
	void getById_DictNotFound()
	{
		assertThrows(DictNotFoundException.class, () -> dictService.getById("incorrect"));
	}

	@Test
	void create()
	{
		createDict(POSTGRES_DICT_ID);
	}

	@Test
	void create_AlreadyExists()
	{
		createAlreadyExistsDict(POSTGRES_DICT_ID);
	}

	@Test
	void create_withUUIDDictFieldIds()
	{
		createWithUUIDDictFieldIds();
	}

	@Test
	void delete()
	{
		deleteDict(POSTGRES_DICT_ID);
	}

	@Test
	void update()
	{
		updateDict(POSTGRES_DICT_ID);
	}

	@Test
	void update_withRenameDictField()
	{
		renameDictField(POSTGRES_DICT_ID);
	}

	@Test
	void createIndex()
	{
		createDictIndex(POSTGRES_DICT_ID);
	}

	@Test
	void deleteIndex()
	{
		deleteDictIndex(POSTGRES_DICT_ID);
	}

	@Test
	void createConstraint()
	{
		createDictConstraint(POSTGRES_DICT_ID);
	}

	@Test
	void deleteConstraint()
	{
		deleteDictConstraint(POSTGRES_DICT_ID);
	}

	@Test
	void createEnum()
	{
		createDictEnum(POSTGRES_DICT_ID);
	}

	@Test
	void deleteEnum()
	{
		deleteDictEnum(POSTGRES_DICT_ID);
	}

	@Test
	void updateEnum()
	{
		updateDictEnum(POSTGRES_DICT_ID);
	}

	@Test
	@Order(Ordered.HIGHEST_PRECEDENCE)
	void getById()
	{
		getDictById(POSTGRES_DICT_ID);
	}

	@Test
	@Order(Ordered.HIGHEST_PRECEDENCE + 1)
	void getAll()
	{
		assertEquals(11, dictService.getAll().size());
	}
}
