package com.proit.app.database.configuration;

import com.proit.app.database.configuration.ddl.AbstractDDLProvider;
import com.proit.app.database.configuration.properties.PostgresTestProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties(PostgresTestProperties.class)
public class TestPostgresDDLProvider extends AbstractDDLProvider
{
	public TestPostgresDDLProvider(PostgresTestProperties testProperties, DataSource dataSource)
	{
		super("postgres", testProperties.getDdl(), dataSource);
	}
}
