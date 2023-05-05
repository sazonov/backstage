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

package com.proit.app.configuration.jpa;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty("app.datasource.url")
@PropertySource("classpath:ds.properties")
public class DataSourceConfiguration
{
	@Bean
	@Primary
	@ConfigurationProperties(prefix = "app.datasource")
	public DataSourceProperties dataSourceProperties()
	{
		return new DataSourceProperties();
	}

	@Bean
	@Primary
	@ConfigurationProperties(prefix = "app.datasource")
	public DataSource dataSource()
	{
		return dataSourceProperties().initializeDataSourceBuilder().build();
	}

	@Bean
	@ConfigurationProperties(prefix = "app.notimeoutdatasource")
	public DataSourceProperties noTimeoutDataSourceProperties()
	{
		return new DataSourceProperties();
	}

	@Bean
	@ConfigurationProperties(prefix = "app.notimeoutdatasource")
	public DataSource noTimeoutDataSource()
	{
		return noTimeoutDataSourceProperties().initializeDataSourceBuilder().build();
	}

	@Bean
	@ConfigurationProperties(prefix = "app.ddldatasource")
	public DataSourceProperties ddlDataSourceProperties()
	{
		return new DataSourceProperties();
	}

	@Bean
	@ConfigurationProperties(prefix = "app.ddldatasource")
	public DataSource ddlDataSource()
	{
		return ddlDataSourceProperties().initializeDataSourceBuilder().build();
	}

	@Bean
	@Primary
	public NamedParameterJdbcTemplate namedParameterJdbcTemplate()
	{
		return new NamedParameterJdbcTemplate(dataSource());
	}

	@Bean(name = "noTimeoutJdbcTemplate")
	public NamedParameterJdbcTemplate noTimeoutJdbcTemplate()
	{
		return new NamedParameterJdbcTemplate(noTimeoutDataSource());
	}

	@Bean
	public PlatformTransactionManager noTimeoutDataSourceTransactionManager()
	{
		return new DataSourceTransactionManager(noTimeoutDataSource());
	}
}
