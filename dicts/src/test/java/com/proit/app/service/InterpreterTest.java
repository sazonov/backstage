package com.proit.app.service;

import com.proit.app.common.AbstractTest;
import com.proit.app.service.ddl.Interpreter;
import com.proit.app.service.ddl.SqlParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class InterpreterTest extends AbstractTest
{
	@Autowired
	private SqlParser sqlParser;

	@Autowired
	private Interpreter interpreter;

	private static final String QUERY = """
			create table document['Документ']
			(
				name['Название'] 			text	not null,
				type						text	not null,
				description['Описание'] 	text,
				link['Ссылка'] 				text	not null,
				externalLink['Внешняя сслка'] text  not null,
				category['Категория'] 		text);

				insert into document (name, type, description, link, externalLink, category) values ('Тестовое', 'REPORT', 'Описание', 'https://yandex.ru', '32d20ff9-b0d2-49b1-8c2d-03e3a9109134', 'Тестовая');
				insert into document (name, type, description, link, externalLink, category) values ('Тестовое', 'VIDEOS', 'Описание', 'https://yandex.ru', '32d20ff9-b0d2-49b1-8c2d-03e3a9109134', 'Тестовая');

			    update document set link = externalLink where type = 'REPORT';
			""";

	@Test
	void updateByColumnValueTest()
	{
		interpreter.execute(sqlParser.parse(QUERY));

		var result = dictDataService.getByFilter("document", List.of("*"), null, PageRequest.of(0, 10));

		assertEquals(result.getTotalElements(), 2L);

		var report = dictDataService.getByFilter("document", List.of("*"), "type = 'REPORT'", PageRequest.of(0, 10));

		assertEquals(report.getTotalElements(), 1L);
		assertEquals(report.getContent().get(0).getData().get("link"), report.getContent().get(0).getData().get("externalLink"));

		var videos = dictDataService.getByFilter("document", List.of("*"), "type = 'VIDEOS'", PageRequest.of(0, 10));

		assertEquals(videos.getTotalElements(), 1L);
		assertNotEquals(videos.getContent().get(0).getData().get("link"), videos.getContent().get(0).getData().get("externalLink"));
	}

	@Test
	void parse_updatePositiveCondition()
	{
		interpreter.execute(sqlParser.parse("""
					update user
					set type = 'EmbeddedReactiveStreamsMongoClientDependsOnBeanFactoryPostProcessor'
					where first_name = 'Bender';
				"""));

		long actual = dictDataService.countByFilter("user", "type = 'EmbeddedReactiveStreamsMongoClientDependsOnBeanFactoryPostProcessor'");

		assertEquals(1, actual);
	}

	@Test
	void parse_updateNegativeCondition()
	{
		interpreter.execute(sqlParser.parse("""
				update user
				set type = 'PropertySourceOrderingBeanFactoryPostProcessor'
				where first_name = 'Bender';
				"""));

		long actual = dictDataService.countByFilter("user", "first_name = 'Bender' and type = 'PropertySourceOrderingBeanFactoryPostProcessor'");

		assertEquals(1, actual);
	}

	@Test
	void parse_updateWithoutCondition()
	{
		interpreter.execute(sqlParser.parse("""
				update user
				set type = 'COMPONENTS';
				"""));

		long actual = dictDataService.countByFilter("user", "type = 'COMPONENTS'");

		assertEquals(2, actual);
	}

	@Test
	void parse_deleteNegativeCondition()
	{
		interpreter.execute(sqlParser.parse("""
				delete from user
				where first_name !='Bender';
				"""));

		long actual = dictDataService.countByFilter("user", "first_name !='Bender' and deleted != null");

		assertEquals(1, actual);
	}

	@Test
	void parse_deleteWithoutCondition()
	{
		interpreter.execute(sqlParser.parse("delete from user;"));

		long actual = dictDataService.countByFilter("user", "deleted = null");

		assertEquals(0, actual);
	}
}
