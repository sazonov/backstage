package com.proit.app.attachment.configuration;

import com.proit.app.attachment.configuration.properties.AttachmentProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AttachmentProperties.class)
public class AttachmentConfiguration
{
}
