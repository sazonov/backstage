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
import com.proit.app.dict.service.CommonClasspathMigrationServiceTest;
import com.proit.app.dict.service.backend.postgres.PostgresEngine;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Order(TestPipeline.POSTGRES_CLASSPATH_MIGRATION)
@PostgresStorage
public class PostgresClasspathMigrationTest extends CommonClasspathMigrationServiceTest
{
	@BeforeAll
	public void init()
	{
		super.init(PostgresEngine.POSTGRES);
	}

	@Test
	void migrate_withMultipleEngines()
	{
		migrateMultipleEngines();
	}

	@Test
	void migrate_Exc()
	{
		migrateExc();
	}

	@Test
	void migrate_CommitSuccess()
	{
		migrateCommitSuccess();
	}

	@Test
	void migrate_RollbackSuccess()
	{
		migrateRollbackSuccess();
	}

	@Test
	void migrate_Json()
	{
		migrateJson();
	}

	@Test
	void migrate_GeoJson()
	{
		migrateGeoJson();
	}

	@Test
	void migrate_withReservedKeywords()
	{
		var keywordsMap = new LinkedHashMap<String, String>();

		keywordsMap.put(
				"keywordsDdl", """
						create table "order"['Ключевое слово'] (name['Имя'] text) engine = 'postgres';
						create table "primary"['Ключевое слово'] (description['Описание'] text) engine = 'postgres';
						alter table order add column "collate"['Ключевое слово'] text;
						alter table order add constraint collateConstraint unique("collate");
						create index collateIndex on order ("collate");
						alter table "order" rename column "collate" to "check"['Ключевое слово'];
						alter table "order" drop column "check";
						create index descIndex on "primary" (description);
						""");
		keywordsMap.put(
				"keywordsDml", """
						insert into "order" values ('name');
						insert into primary values ('description');
						update order set name = 'updated_name';
						update "primary" set description = 'updated_desc';
						delete from order;
						delete from "primary";
						""");

		keywordsMap.entrySet().forEach(classpathMigrationService::migrate);

		var dicts = dictService.getAll()
				.stream()
				.filter(it -> StringUtils.equalsAny(it.getId(), "order", "primary"))
				.toList();

		assertEquals(2, dicts.size());
	}

	@Test
	void migrate_withSelfJoinDict()
	{
		migrateSelfJoin();
	}
}
