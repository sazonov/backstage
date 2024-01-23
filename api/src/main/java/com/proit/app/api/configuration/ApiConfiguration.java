package com.proit.app.api.configuration;

import com.proit.app.api.configuration.properties.ApiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ApiProperties.class)
public class ApiConfiguration
{
}
