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

package com.proit.app.database.configuration.properties;

import com.clickhouse.client.http.config.HttpConnectionProvider;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties("app.clickhouse")
public class ClickHouseProperties
{
	public static final String ACTIVATION_PROPERTY = "app.clickhouse.host";

	private String host;

	private Integer port;

	private String username;

	private String password;

	private String database;

	private HttpConnectionProvider connectionProvider = HttpConnectionProvider.HTTP_URL_CONNECTION;

	/**
	 * Максимальный размер пула соединений.
	 */
	private int maxPoolSize = 5;

	/**
	 * Максимальное время жизни соединения.
	 */
	private Duration maxLifetime = Duration.ofSeconds(300);

	private Duration connectionTimeout = Duration.ofSeconds(600);
}
