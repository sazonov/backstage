package com.proit.app.report;

import com.proit.app.database.configuration.jpa.DataSourceConfiguration;
import com.proit.app.database.configuration.jpa.JpaConfiguration;
import com.proit.app.report.configuration.ReportConfiguration;
import org.junit.ClassRule;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDate;

@SpringBootTest
@ContextConfiguration(classes = TestApp.class, initializers = {AbstractTest.Initializer.class})
@Import({ReportConfiguration.class, DataSourceConfiguration.class, JpaConfiguration.class, JacksonAutoConfiguration.class, JmsAutoConfiguration.class})
public class AbstractTest
{
	protected static final LocalDate FROM = LocalDate.of(2000, 1, 1);
	protected static final LocalDate TO = LocalDate.of(2000, 12, 31);

	@ClassRule
	public static final PostgreSQLContainer<?> postgres;

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
