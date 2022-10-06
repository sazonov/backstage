package com.proit.app.common;

import org.junit.ClassRule;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ContextConfiguration(initializers = {AbstractTests.Initializer.class})
@ImportAutoConfiguration(JmsAutoConfiguration.class)
public abstract class AbstractTests
{
	@ClassRule
	static final PostgreSQLContainer<?> postgres;

	static
	{
		postgres = new PostgreSQLContainer<>("postgres");
		postgres.start();
	}

	static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext>
	{
		@Override
		public void initialize(ConfigurableApplicationContext configurableApplicationContext)
		{
			TestPropertyValues.of(
					"app.dataSource.driverClassName=" + postgres.getDriverClassName(),
					"app.dataSource.url=" + postgres.getJdbcUrl(),
					"app.dataSource.username=" + postgres.getUsername(),
					"app.dataSource.password=" + postgres.getPassword()
			).applyTo(configurableApplicationContext.getEnvironment());
		}
	}
}
