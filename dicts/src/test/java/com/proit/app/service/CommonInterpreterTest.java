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
import com.proit.app.service.ddl.Interpreter;
import com.proit.app.service.ddl.SqlParser;
import org.apache.commons.lang.text.StrSubstitutor;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommonInterpreterTest extends CommonTest
{
	protected static String TESTABLE_DICT_ID;

	@Autowired
	private SqlParser sqlParser;

	@Autowired
	private Interpreter interpreter;

	protected void buildTestableDictHierarchy(String storageDictId)
	{
		TESTABLE_DICT_ID = storageDictId + "superUser";

		var query = """
				create table %s['Супер_Пользователи']
				(
				    "type"['Тип'] text,
				    last_name['Фамилия'] text,
				    first_name['Имя'] text,
				    middle_name['Отчество'] text,
				    contract['Договор'] attachment,
				    birth_date['Дата рождения'] date);

				    insert into %s values('OPERATOR', 'Bending', 'Bender', 'Rodríguez', null, '2020-01-01');
				    insert into %s values('MANAGER', 'Jay', 'Homer', 'Simpson', null, '2021-01-02');
				""".formatted(TESTABLE_DICT_ID, TESTABLE_DICT_ID, TESTABLE_DICT_ID);

		interpreter.execute(sqlParser.parse(query));
	}

	protected void updateByColumnValue(String storageDictId)
	{
		var dictId = storageDictId + "document";

		var query = """
				create table ${dictId}['Документ']
				(
					name['Название'] 			text	not null,
					type						text	not null,
					description['Описание'] 	text,
					link['Ссылка'] 				text	not null,
					externalLink['Внешняя сслка'] text  not null,
					category['Категория'] 		text);

					insert into ${dictId} (name, type, description, link, externalLink, category) values ('Тестовое', 'REPORT', 'Описание', 'https://yandex.ru', '32d20ff9-b0d2-49b1-8c2d-03e3a9109134', 'Тестовая');
					insert into ${dictId} (name, type, description, link, externalLink, category) values ('Тестовое', 'VIDEOS', 'Описание', 'https://yandex.ru', '32d20ff9-b0d2-49b1-8c2d-03e3a9109134', 'Тестовая');

				    update ${dictId} set link = externalLink where type = 'REPORT';
				""";

		var dataLanguage = StrSubstitutor.replace(query, Map.of("dictId", dictId), "${", "}");

		interpreter.execute(sqlParser.parse(dataLanguage));

		var result = dictDataService.getByFilter(dictId, List.of("*"), null, PageRequest.of(0, 10));

		assertEquals(result.getTotalElements(), 2L);

		var report = dictDataService.getByFilter(dictId, List.of("*"), "type = 'REPORT'", PageRequest.of(0, 10));

		assertEquals(report.getTotalElements(), 1L);
		assertEquals(report.getContent().get(0).getData().get("link"), report.getContent().get(0).getData().get("externalLink"));

		var videos = dictDataService.getByFilter(dictId, List.of("*"), "type = 'VIDEOS'", PageRequest.of(0, 10));

		assertEquals(videos.getTotalElements(), 1L);
		assertNotEquals(videos.getContent().get(0).getData().get("link"), videos.getContent().get(0).getData().get("externalLink"));
	}

	protected void parseUpdatePositiveCondition()
	{
		interpreter.execute(sqlParser.parse("""
					update %s
					set type = 'EmbeddedReactiveStreamsMongoClientDependsOnBeanFactoryPostProcessor'
					where first_name = 'Bender';
				""".formatted(TESTABLE_DICT_ID)));

		long actual = dictDataService.countByFilter(TESTABLE_DICT_ID, "type = 'EmbeddedReactiveStreamsMongoClientDependsOnBeanFactoryPostProcessor'");

		assertEquals(1, actual);
	}

	protected void parseUpdateNegativeCondition()
	{
		interpreter.execute(sqlParser.parse("""
				update %s
				set type = 'PropertySourceOrderingBeanFactoryPostProcessor'
				where first_name = 'Bender';
				""".formatted(TESTABLE_DICT_ID)));

		long actual = dictDataService.countByFilter(TESTABLE_DICT_ID, "first_name = 'Bender' and type = 'PropertySourceOrderingBeanFactoryPostProcessor'");

		assertEquals(1, actual);
	}

	protected void parseUpdateWithoutCondition()
	{
		interpreter.execute(sqlParser.parse("""
				update %s
				set type = 'COMPONENTS';
				""".formatted(TESTABLE_DICT_ID)));

		long actual = dictDataService.countByFilter(TESTABLE_DICT_ID, "type = 'COMPONENTS'");

		assertEquals(2, actual);
	}

	protected void parseDeleteNegativeCondition()
	{
		interpreter.execute(sqlParser.parse("""
				delete from %s
				where first_name !='Bender';
				""".formatted(TESTABLE_DICT_ID)));

		long actual = dictDataService.countByFilter(TESTABLE_DICT_ID, "first_name !='Bender' and deleted != null");

		assertEquals(1, actual);
	}

	protected void parseDeleteWithoutCondition()
	{
		interpreter.execute(sqlParser.parse("delete from %s;".formatted(TESTABLE_DICT_ID)));

		long actual = dictDataService.countByFilter(TESTABLE_DICT_ID, "deleted = null");

		assertEquals(0, actual);
	}
}
