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

package com.proit.app.service;

import com.proit.app.common.CommonTest;
import com.proit.app.domain.Dict;
import com.proit.app.domain.DictItem;
import com.proit.app.exception.migration.MigrationAppliedException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.TestInstance;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommonClasspathMigrationServiceTest extends CommonTest
{
	private static String TESTABLE_ENGINE_NAME;

	protected void init(String engineName)
	{
		TESTABLE_ENGINE_NAME = engineName;
	}

	protected void migrateMultipleEngines()
	{
		assertThrows(MigrationAppliedException.class, () -> buildMultipleEnginesMap().entrySet()
				.forEach(classpathMigrationService::migrate));
	}

	protected void migrateExc()
	{
		assertThrows(RuntimeException.class, () -> classpathMigrationService.migrate(new AbstractMap.SimpleEntry<>("incorrectName", null)));
	}

	protected void migrateCommitSuccess()
	{
		var migrationMap = buildCommitMap();

		var commitDictIds = migrationMap.keySet()
				.stream()
				.filter(it -> StringUtils.startsWith(it, "commit"))
				.collect(Collectors.toSet());

		var commitDictData = Set.of("commit1_name_1", "commit1_type_1");

		var withoutCommitDictIds = dictService.getAll()
				.stream()
				.map(Dict::getId)
				.collect(Collectors.toSet());

		var notExists = !withoutCommitDictIds.containsAll(commitDictIds);

		assertTrue(notExists);

		migrationMap.entrySet().forEach(classpathMigrationService::migrate);

		var withCommitDictIds = dictService.getAll()
				.stream()
				.map(Dict::getId)
				.collect(Collectors.toSet());

		var successCommit = withCommitDictIds.containsAll(commitDictIds);

		var firstDictId = commitDictIds.stream()
				.filter(it -> StringUtils.startsWith(it, "commit1"))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Migrate with commit test failed."));

		var successDictDataCommit = dictDataService.getByFilter(firstDictId, List.of("name", "type"), null, PageRequest.of(0, 100))
				.getContent()
				.stream()
				.map(DictItem::getData)
				.map(Map::values)
				.flatMap(Collection::stream)
				.map(Object::toString)
				.allMatch(commitDictData::contains);

		assertTrue(successCommit);
		assertTrue(successDictDataCommit);
	}

	protected void migrateRollbackSuccess()
	{
		var migrationMap = buildRollbackMap();

		var rollbackDictData = Set.of("some_name_rollback", "some_type_rollback");

		var commitDictIds = migrationMap.entrySet()
				.stream()
				.filter(Predicate.not(it -> StringUtils.equals(it.getKey(), "rollback")))
				.peek(classpathMigrationService::migrate)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());

		var rollback = migrationMap.entrySet()
				.stream()
				.filter(it -> StringUtils.equals(it.getKey(), "rollback"))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Migrate with rollback test failed."));

		assertThrows(MigrationAppliedException.class, () -> classpathMigrationService.migrate(rollback));

		var dictIds = dictService.getAll()
				.stream()
				.map(Dict::getId)
				.collect(Collectors.toSet());

		var successCommit = dictIds.containsAll(commitDictIds);
		var successRollback = !dictIds.contains("rollback");

		var firstDictId = commitDictIds.stream()
				.filter(it -> StringUtils.startsWith(it, "commit1"))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Migrate with rollback test failed."));

		var successRollbackDictData = dictDataService.getByFilter(firstDictId, List.of("name", "type"), null, PageRequest.of(0, 100))
				.getContent()
				.stream()
				.map(DictItem::getData)
				.map(Map::values)
				.flatMap(Collection::stream)
				.map(Object::toString)
				.noneMatch(rollbackDictData::contains);

		assertTrue(successCommit);
		assertTrue(successRollback);
		assertTrue(successRollbackDictData);
	}

	protected void migrateJson()
	{
		var dictId = withRandom("migrate_json%s".formatted(TESTABLE_ENGINE_NAME));

		var migration = Map.of("json", """
				create table %s['Миграция_json']
				(
					number['Номер'] int,
					data json,
					comments['Комментарии'] json[]
				);

				insert into %s values (1, '{"day":10, "night":17, "region": "Moscow"}'::json, null);
				insert into %s values (2, null, array['{"userId": 17, "comment": "comment"}'::json, '{"userId": 20, "comment": "comment"}'::json]);

				update %s set comments = array['{"userId": 17, "comment": "comment17"}'::json, '{"userId": 20, "comment": "comment20"}'::json] where number = 1;
				update %s set data = '{"day":15, "night":22, "region": "Sochi"}'::json where number = 2;
				""".formatted(dictId, dictId, dictId, dictId, dictId));

		migration.entrySet().forEach(classpathMigrationService::migrate);

		assertTrue(dictDataService.existsByFilter(dictId, "number = 1"));
		assertTrue(dictDataService.existsByFilter(dictId, "number = 2"));
	}

	protected void migrateGeoJson()
	{
		var dictId = withRandom("migrate_geo_json%s".formatted(TESTABLE_ENGINE_NAME));

		var migration = Map.of("geo_json", """
				create table %s['Миграция_geo_json']
				(
					number['Порядковый номер'] int,
					geom['Геометрия'] geo_json
				);

				insert into %s values (1, '{"type":"Feature","geometry":{"type":"Polygon","coordinates":[[[55.68154659727316,37.553090921089115],[55.646161942996564,37.58087155165372]]]}}');
				""".formatted(dictId, dictId));

		migration.entrySet().forEach(classpathMigrationService::migrate);

		assertTrue(dictDataService.existsByFilter(dictId, "number = 1"));
	}

	protected void migrateSelfJoin()
	{
		var migrationMap = new LinkedHashMap<String, String>();

		var dictId = withRandom("selfJoin_%s".formatted(TESTABLE_ENGINE_NAME));

		migrationMap.put(
				"selfJoinDdl", """
						create table %1$s
						(
							name['Название'] text not null
						);

						alter table %1$s
							add column parentId['Id родителя'] text references %1$s;
						""".formatted(dictId));
		migrationMap.put(
				"selfJoinDml", """
						insert into %1$s (id, name, parentId)
						values ('parent', 'Родитель', null);
						insert into %1$s (id, name, parentId)
						values ('child', 'Потомок', 'parent');
						""".formatted(dictId));

		migrationMap.entrySet().forEach(classpathMigrationService::migrate);

		var child = dictDataService.getByFilter(dictId, List.of("*"), "parentId = 'parent'", Pageable.unpaged())
				.getContent()
				.get(0)
				.getId();

		var parent = dictDataService.getByFilter(dictId, List.of("*"), "parentId = null", Pageable.unpaged())
				.getContent()
				.get(0)
				.getId();

		assertEquals("child", child);
		assertEquals("parent", parent);
	}

	private Map<String, String> buildCommitMap()
	{
		var commitMap = new LinkedHashMap<String, String>();

		var firstDict = withRandom("commit1%s".formatted(TESTABLE_ENGINE_NAME));
		var secondDict = withRandom("commit2%s".formatted(TESTABLE_ENGINE_NAME));
		var thirdDict = withRandom("commit3%s".formatted(TESTABLE_ENGINE_NAME));

		var firstDdl = """
				create table %s['Коммит'] (name['Название'] text, type['Тип'] text) engine = '%s';
				""".formatted(firstDict, TESTABLE_ENGINE_NAME);

		var secondDdl = """
				create table %s['Коммит'] (name['Название'] text, description['Описание'] text) engine = '%s';
				""".formatted(secondDict, TESTABLE_ENGINE_NAME);

		var thirdDdl = """
				create table %s['Коммит'] (name['Название'] text, type['Тип'] text) engine = '%s';
				alter table %s rename column name to commit_name;
				""".formatted(thirdDict, TESTABLE_ENGINE_NAME, thirdDict);

		var dml = """
				insert into %s values ('commit1_name_1', 'commit1_type_1');
				""".formatted(firstDict);

		commitMap.put(firstDict, firstDdl);
		commitMap.put(secondDict, secondDdl);
		commitMap.put(thirdDict, thirdDdl);
		commitMap.put("dml", dml);

		return commitMap;
	}

	private Map<String, String> buildRollbackMap()
	{
		var rollbackMap = new LinkedHashMap<String, String>();

		var firstDict = withRandom("commit1%s".formatted(TESTABLE_ENGINE_NAME));
		var secondDict = withRandom("commit2%s".formatted(TESTABLE_ENGINE_NAME));

		var firstCommitDdl = """
				create table %s['Коммит_с_откатом'] (name['Название'] text, type['Тип'] text) engine = '%s';
				""".formatted(firstDict, TESTABLE_ENGINE_NAME);

		var secondCommitDdl = """
				create table %s['Коммит_с_откатом'] (name['Название'] text, description['Описание'] text) engine = '%s';
				""".formatted(secondDict, TESTABLE_ENGINE_NAME);

		var rollbackDdl = """
				create table rollback['Откат'] (name['Название'] text, type['Тип'] text) engine = '%s';
				insert into %s values ('some_name_rollback', 'some_type_rollback');
				insert into %s values ('some_name_rollback', 'some_type_rollback');
				alter table rollback rename column name to rollbackName;
				alter table rollback rename column rollbackNameIncorrect to name;
				""".formatted(TESTABLE_ENGINE_NAME, firstDict, secondDict);

		rollbackMap.put(firstDict, firstCommitDdl);
		rollbackMap.put(secondDict, secondCommitDdl);
		rollbackMap.put("rollback", rollbackDdl);

		return rollbackMap;
	}

	private Map<String, String> buildMultipleEnginesMap()
	{
		var multipleEnginesMap = new LinkedHashMap<String, String>();

		var ddl = """
				create table multipleEngine1Mongo['Коммит_с_откатом'] (name['Название'] text, "type"['Тип'] text) engine = 'mongo';
				create table multipleEngine2Pg['Коммит_с_откатом'] (name['Название'] text, description['Описание'] text) engine = 'postgres';
				""";

		multipleEnginesMap.put("multipleEngines", ddl);

		return multipleEnginesMap;
	}
}
