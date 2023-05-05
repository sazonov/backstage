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
	public static final PostgreSQLContainer<?> postgres;

	static
	{
		postgres = new PostgreSQLContainer<>("postgres");

		postgres.start();
	}

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext)
	{
		TestPropertyValues.of(
				"app.dataSource.driverClassName=" + postgres.getDriverClassName(),
				"app.dataSource.url=" + postgres.getJdbcUrl(),
				"app.dataSource.username=" + postgres.getUsername(),
				"app.dataSource.password=" + postgres.getPassword()
		).applyTo(applicationContext.getEnvironment());
	}
}
