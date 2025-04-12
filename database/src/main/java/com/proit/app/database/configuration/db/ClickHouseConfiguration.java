/*
 *    Copyright 2019-2024 the original author or authors.
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

package com.proit.app.database.configuration.db;

import com.clickhouse.client.http.config.ClickHouseHttpOption;
import com.clickhouse.jdbc.ClickHouseDataSource;
import com.proit.app.database.configuration.properties.ClickHouseProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(ClickHouseProperties.class)
@ConditionalOnProperty(ClickHouseProperties.ACTIVATION_PROPERTY)
public class ClickHouseConfiguration
{
	@Bean
	public NamedParameterJdbcTemplate clickHouseJdbcTemplate(@Qualifier("clickHouseDataSource") DataSource clickHouseDataSource)
	{
		return new NamedParameterJdbcTemplate(clickHouseDataSource);
	}

	@Bean
	public DataSource clickHouseDdlDataSource(ClickHouseProperties properties) throws SQLException
	{
		var poolConfig = buildPoolConfig("ClickHouseDdl-HikariPool", 4, 0, 0,
				rawClickHouseDataSource(properties), properties.getUsername(), properties.getPassword());

		return new HikariDataSource(poolConfig);
	}

	@Bean
	public DataSource clickHouseDataSource(ClickHouseProperties properties) throws SQLException
	{
		var poolConfig = buildPoolConfig("ClickHouse-HikariPool", properties.getMaxPoolSize(),
				properties.getMaxLifetime().toMillis(), properties.getConnectionTimeout().toMillis(),
				rawClickHouseDataSource(properties), properties.getUsername(), properties.getPassword());

		return new HikariDataSource(poolConfig);
	}

	private HikariConfig buildPoolConfig(String poolName, int maximumPoolSize, long maxLifetime, long connectionTimeout,
	                                     DataSource dataSource, String username, String password)
	{
		var poolConfig = new HikariConfig();
		poolConfig.setPoolName(poolName);
		poolConfig.setMaximumPoolSize(maximumPoolSize);
		poolConfig.setMaxLifetime(maxLifetime);
		poolConfig.setConnectionTimeout(connectionTimeout);
		poolConfig.setDataSource(dataSource);
		poolConfig.setUsername(username);
		poolConfig.setPassword(password);

		return poolConfig;
	}

	private ClickHouseDataSource rawClickHouseDataSource(ClickHouseProperties properties) throws SQLException
	{
		var clientProps = new Properties();
		clientProps.setProperty(ClickHouseHttpOption.CONNECTION_PROVIDER.getKey(), properties.getConnectionProvider().name());

		return new ClickHouseDataSource(buildDataSourceUrl(properties), clientProps);
	}

	private String buildDataSourceUrl(ClickHouseProperties properties)
	{
		return buildDataSourceUrl(properties.getHost(), properties.getPort(), properties.getDatabase());
	}

	private String buildDataSourceUrl(String host, Integer port, String database)
	{
		var url = String.format("jdbc:clickhouse://%s", host);

		if (port != null)
		{
			url += (":" + port);
		}

		if (StringUtils.isNotBlank(database))
		{
			url += ("/" + database);
		}

		return url;
	}
}
