package com.proit.app.database.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("app.test.postgres")
public class PostgresTestProperties
{
	private DDLProperties ddl = new DDLProperties();
}
