package com.proit.app.attachment.configuration;

import com.proit.app.attachment.configuration.properties.AttachmentProperties;
import com.proit.app.database.configuration.ddl.AbstractDDLProvider;
import com.proit.app.database.configuration.ddl.DDLConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(value = AttachmentProperties.DDL_ACTIVATION_PROPERTY, matchIfMissing = true)
@Order(AttachmentDDLProvider.PRECEDENCE)
public class AttachmentDDLProvider extends AbstractDDLProvider
{
	public static final int PRECEDENCE = DDLConfiguration.DDL_PRECEDENCE_SYSTEM + 70;

	@Autowired
	public AttachmentDDLProvider(@Qualifier("ddlDataSource") DataSource dataSource)
	{
		super("attachments", dataSource);
	}
}
