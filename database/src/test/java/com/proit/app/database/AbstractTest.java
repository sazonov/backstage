package com.proit.app.database;

import com.proit.app.database.configuration.jpa.DataSourceConfiguration;
import com.proit.app.database.configuration.jpa.JpaConfiguration;
import org.junit.ClassRule;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ContextConfiguration(classes = TestApp.class, initializers = {AbstractTest.Initializer.class})
@Import({DataSourceConfiguration.class, JpaConfiguration.class, JacksonAutoConfiguration.class})
public class AbstractTest
{
	public static final String POSTGRES_IMAGE_NAME = "postgres:14.9";

	@ClassRule
	public static final PostgreSQLContainer<?> postgres;

	@ClassRule
	public static final ClickHouseContainer clickhouse;

	static
	{
		postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE_NAME);
		postgres.start();

		clickhouse = new ClickHouseContainer("clickhouse/clickhouse-server:23.9.2.56");
		clickhouse.start();
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
					"app.clickhouse.host=" + clickhouse.getHost(),
					"app.clickhouse.port=" + clickhouse.getFirstMappedPort()
			).applyTo(configurableApplicationContext.getEnvironment());
		}
	}

	private static DockerImageName buildCustomPgImage()
	{
		var customPostgresImage = new ImageFromDockerfile()
				.withDockerfileFromBuilder(builder ->
						builder
								.from(POSTGRES_IMAGE_NAME)
								.env("DEBIAN_FRONTEND", "noninteractive")
								.run("apt-get update && apt-get install -y postgis postgresql-14-postgis-3 postgresql-14-partman --force-yes")
								.cmd("/usr/local/bin/docker-entrypoint.sh", "postgres")
								.build());

		return DockerImageName.parse(customPostgresImage.get())
				.asCompatibleSubstituteFor(PostgreSQLContainer.IMAGE);
	}
}
