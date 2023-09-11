/*
 *    Copyright 2019-2023 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.proit.app.configuration;

import com.proit.app.service.backend.postgres.PostgresReservedKeyword;
import com.proit.app.service.ddl.SqlParser;
import com.proit.app.service.query.MongoTranslator;
import com.proit.app.service.query.PostgresTranslator;
import com.proit.app.service.query.QueryParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TranslatorConfiguration
{
	@Bean
	public QueryParser queryParser()
	{
		return new QueryParser();
	}

	@Bean
	public MongoTranslator mongoTranslator()
	{
		return new MongoTranslator();
	}

	@Bean
	public PostgresTranslator postgresTranslator(PostgresReservedKeyword postgresReservedKeyword)
	{
		return new PostgresTranslator(postgresReservedKeyword);
	}

	@Bean
	public SqlParser sqlParser()
	{
		return new SqlParser();
	}
}
