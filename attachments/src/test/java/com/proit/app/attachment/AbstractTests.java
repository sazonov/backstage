package com.proit.app.attachment;

import org.junit.ClassRule;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;

@WebMvcTest
@ContextConfiguration(classes = TestApplication.class, initializers = {AbstractTests.Initializer.class})
@ImportAutoConfiguration(classes = {JmsAutoConfiguration.class, JacksonAutoConfiguration.class})
public abstract class AbstractTests
{
	@ClassRule
	static final PostgreSQLContainer<?> postgres;

	@ClassRule
	static final MinIOContainer minio;

	static
	{
		postgres = new PostgreSQLContainer<>("postgres");
		postgres.start();

		minio = new MinIOContainer("minio/minio");
		minio.start();
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

			TestPropertyValues.of(
					"app.attachments.minio.endpoint=" + minio.getS3URL(),
					"app.attachments.minio.access-key=" + minio.getUserName(),
					"app.attachments.minio.secret-key=" + minio.getPassword()
			).applyTo(configurableApplicationContext.getEnvironment());
		}
	}
}
