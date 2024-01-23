package com.proit.app.audit.configuration;

import com.proit.app.audit.configuration.properties.AuditProperties;
import com.proit.app.database.configuration.ddl.AbstractDDLProvider;
import com.proit.app.database.configuration.ddl.DDLConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(value = AuditProperties.DDL_ACTIVATION_PROPERTY, matchIfMissing = true)
@Order(AuditDDLProvider.PRECEDENCE)
public class AuditDDLProvider extends AbstractDDLProvider
{
	public static final int PRECEDENCE = DDLConfiguration.DDL_PRECEDENCE_SYSTEM + 80;

	@Autowired
	public AuditDDLProvider(@Qualifier("ddlDataSource") DataSource dataSource)
	{
		super("audit", dataSource);
	}
}
