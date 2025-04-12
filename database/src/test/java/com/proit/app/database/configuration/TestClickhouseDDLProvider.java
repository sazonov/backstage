package com.proit.app.database.configuration;

import com.proit.app.database.configuration.ddl.AbstractDDLProvider;
import com.proit.app.database.configuration.properties.ClickHouseProperties;
import com.proit.app.database.configuration.properties.ClickhouseTestProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(ClickhouseTestProperties.class)
@ConditionalOnProperty(ClickHouseProperties.ACTIVATION_PROPERTY)
public class TestClickhouseDDLProvider extends AbstractDDLProvider
{
	public TestClickhouseDDLProvider(ClickhouseTestProperties testProperties,
	                                 @Qualifier("clickHouseDdlDataSource") DataSource dataSource)
	{
		super("clickhouse", testProperties.getDdl(), dataSource);
	}
}
