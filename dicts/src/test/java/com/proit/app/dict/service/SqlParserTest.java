package com.proit.app.dict.service;

import com.proit.app.dict.common.CommonTest;
import com.proit.app.dict.service.ddl.SqlParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlParserTest extends CommonTest
{
	@Autowired
	private SqlParser sqlParser;

	private static final String EMPTY_QUERY = "";
	private static final String COMMENT_QUERY = """
			-- Комментарий 1.
			/* Комментарий 2.*/

			-- Комментарий 3.
			""";

	private static final String CREATE_TABLE_QUERY = """
			/* Роли */
			create table roles['Роли'] (id['Идентификатор'] text, name['Название'] text, comments['Комментарии'] text[]);

			-- Пользователи.
			create table users['Пользователи'] (
			    name['Имя'] text,
			    lastName['фамилия'] text,
			    age['Возраст'] int,
			    role['Роль'] text not null references roles,
			    registrationDate['Дата регистрации'] date);
			""";

	private static final String ALTER_TABLE_QUERY = """
			alter table users add column comments['Комментарии'] text[];
			alter table roles drop column comments;
			alter table roles add constraint constrName unique (field);
			alter table roles add constraint constrName unique (field, field2);
			alter table roles drop constraint constrName;
			""";

	private static final String INSERT_QUERY = """
			insert into roles (id, name, comments) values ('MANAGER', 'Менеджер', ARRAY['11', '22', '33']);
			insert into users values('Иван', 'Иванов', 20, 'MANAGER', '2020-08-20');
			""";

	private static final String UPDATE_QUERY = """
			update users set name = 'Василий', age = 30 where id = 1;
			update users set name = 'Николай' where id = '2';
			""";

	private static final String UPDATE_BY_COLUMN_VALUE_QUERY = """
			update users set lastName = lastName where id = 1;
			""";

	private static final String DELETE_QUERY = """
			delete from users where id = 1;
			""";

	private static final String CREATE_INDEX_QUERY = """
			create index usersLastName on users (lastName);
			create index usersUniqueName on users (firstName, lastName);
			create index usersFirstName on users (firstName) desc;
			""";

	private static final String DELETE_INDEX_QUERY = """
			drop index usersLastName on users;
			""";

	@Test
	void parse_emptyQuery()
	{
		assertTrue(sqlParser.parse(EMPTY_QUERY).isEmpty());
	}

	@Test
	void parse_commentQuery()
	{
		assertTrue(sqlParser.parse(COMMENT_QUERY).isEmpty());
	}

	@Test
	void parse_createTableQuery()
	{
		assertEquals(sqlParser.parse(CREATE_TABLE_QUERY).size(), 2);
	}

	@Test
	void parse_alterTableQuery()
	{
		assertEquals(sqlParser.parse(ALTER_TABLE_QUERY).size(), 5);
	}

	@Test
	void parse_insertQuery()
	{
		assertEquals(sqlParser.parse(INSERT_QUERY).size(), 2);
	}

	@Test
	void parse_updateQuery()
	{
		assertEquals(sqlParser.parse(UPDATE_QUERY).size(), 2);
	}

	@Test
	void parse_updateByColumnValueQuery()
	{
		assertEquals(sqlParser.parse(UPDATE_BY_COLUMN_VALUE_QUERY).size(), 1);
	}

	@Test
	void parse_deleteQuery()
	{
		assertEquals(sqlParser.parse(DELETE_QUERY).size(), 1);
	}

	@Test
	void parse_createIndex()
	{
		assertEquals(sqlParser.parse(CREATE_INDEX_QUERY).size(), 3);
	}

	@Test
	void parse_deleteIndex()
	{
		assertEquals(sqlParser.parse(DELETE_INDEX_QUERY).size(), 1);
	}
}