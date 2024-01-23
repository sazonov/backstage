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

package com.proit.app.dict.service.backend.postgres;

import com.proit.app.dict.configuration.properties.DictsProperties;
import com.proit.app.dict.domain.DictEngine;
import com.proit.app.dict.service.backend.Engine;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.util.Map;

@Component
public class PostgresEngine implements Engine
{
	public static final String POSTGRES = "postgres";

	private final DictsProperties dictsProperties;

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public PostgresEngine(DictsProperties dictsProperties, @Qualifier("dictJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate)
	{
		this.dictsProperties = dictsProperties;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public DictEngine getDictEngine()
	{
		return new DictEngine(POSTGRES);
	}

	@Override
	public void createSchema()
	{
		var sql = "create schema if not exists %s".formatted(dictsProperties.getScheme());

		jdbcTemplate.update(sql, Map.of());
	}

	@Override
	public void createDict()
	{
		var sql = """
				create table if not exists dict (
					id                varchar(40)   not null,
					name              varchar(500),
					fields            jsonb                  default '[]'::jsonb,
					indexes           jsonb                  default '[]'::jsonb,
					constraints       jsonb                  default '[]'::jsonb,
					enums             jsonb                  default '[]'::jsonb,
					view_permission   varchar(100),
					edit_permission   varchar(100),
					deleted           timestamp,
					engine            varchar(40)   not null,
					primary key (id))
				""";

		jdbcTemplate.update(sql, Map.of());
	}

	@Override
	public void createVersionScheme()
	{
		var sql = """
				create table if not exists version_scheme (
					id                varchar(40)   not null,
					version           varchar(40)   not null,
					script            varchar(500)  not null,
					checksum          varchar(100)  not null,
					installed         timestamp     not null,
					primary key (id))
				""";

		jdbcTemplate.update(sql, Map.of());
	}

	@Override
	public boolean dictExists()
	{
		var sql = "select exists(select from pg_tables where tablename = 'dict' and schemaname = '%s')".formatted(dictsProperties.getScheme());

		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, new EmptySqlParameterSource(), Boolean.class));
	}

	@Override
	public boolean versionSchemeExists()
	{
		var sql = "select exists(select from pg_tables where tablename = 'dict' and schemaname = '%s')".formatted(dictsProperties.getScheme());

		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, new EmptySqlParameterSource(), Boolean.class));
	}

	@Override
	public void dropDict()
	{
		var sql = "drop table if exists dict";

		jdbcTemplate.execute(sql, PreparedStatement::execute);
	}

	@Override
	public void dropVersionScheme()
	{
		var sql = "drop table if exists version_scheme";

		jdbcTemplate.execute(sql, PreparedStatement::execute);
	}
}
