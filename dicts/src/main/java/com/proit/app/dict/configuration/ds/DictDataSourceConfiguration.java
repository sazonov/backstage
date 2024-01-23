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

package com.proit.app.dict.configuration.ds;

import com.proit.app.dict.configuration.properties.DictsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;

//TODO: написать спеку
@Configuration
@ConditionalOnExpression("#{('${app.dicts.datasource.url}' != null || '${app.datasource.url}') != null}")
@RequiredArgsConstructor
public class DictDataSourceConfiguration
{
	private final DictsProperties dictsProperties;

	@Bean
	@ConfigurationProperties(prefix = "app.dicts.datasource")
	public DataSourceProperties dictDataSourceProperties()
	{
		return new DataSourceProperties();
	}

	@Bean
	public DataSource dictDataSource(DataSourceProperties dataSourceProperties)
	{
		var dictDataSourceProperties = dictDataSourceProperties();

		var dataSource = dictDataSourceProperties.getUrl() == null
				? dataSourceProperties.initializeDataSourceBuilder().build()
				: dictDataSourceProperties.initializeDataSourceBuilder().build();

		createDictSchemaIfNotExists(dataSource);

		return new DictSchemaDelegatingDataSource(dataSource, dictsProperties.getScheme());
	}

	@Bean
	public NamedParameterJdbcTemplate dictJdbcTemplate(@Qualifier("dictDataSource") DataSource dictDataSource)
	{
		return new NamedParameterJdbcTemplate(dictDataSource);
	}

	private void createDictSchemaIfNotExists(DataSource dataSource)
	{
		try (var connection = dataSource.getConnection())
		{
			connection.createStatement()
					.execute("create schema if not exists %s".formatted(dictsProperties.getScheme()));
		}
		catch (SQLException e)
		{
			throw new RuntimeException("При создании схемы '%s' источника данных модуля dicts, произошла ошибка: %s"
					.formatted(dictsProperties.getScheme(), e));
		}
	}
}
