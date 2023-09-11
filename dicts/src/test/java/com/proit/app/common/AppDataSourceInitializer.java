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

package com.proit.app.common;

import org.junit.ClassRule;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

public class AppDataSourceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>
{
	@ClassRule
	public static final PostgreSQLContainer<?> appDataSource;

	@ClassRule
	public static final PostgreSQLContainer<?> dictDataSource;

	static
	{
		appDataSource = new PostgreSQLContainer<>("postgres");
		appDataSource.start();

		dictDataSource = new PostgreSQLContainer<>("postgres");
		dictDataSource.start();
	}

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext)
	{
		TestPropertyValues.of(
				"app.dataSource.driverClassName=" + appDataSource.getDriverClassName(),
				"app.dataSource.url=" + appDataSource.getJdbcUrl(),
				"app.dataSource.username=" + appDataSource.getUsername(),
				"app.dataSource.password=" + appDataSource.getPassword()
		).applyTo(applicationContext.getEnvironment());

		TestPropertyValues.of(
				"app.dicts.dataSource.driverClassName=" + dictDataSource.getDriverClassName(),
				"app.dicts.dataSource.url=" + dictDataSource.getJdbcUrl(),
				"app.dicts.dataSource.username=" + dictDataSource.getUsername(),
				"app.dicts.dataSource.password=" + dictDataSource.getPassword()
		).applyTo(applicationContext.getEnvironment());
	}
}
