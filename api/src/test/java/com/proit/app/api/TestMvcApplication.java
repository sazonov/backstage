package com.proit.app.api;

import com.proit.app.api.configuration.properties.ApiProperties;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@ComponentScan(basePackages = "com.proit.app.api.controller")
@Import(ApiProperties.class)
@SpringBootConfiguration
@ImportAutoConfiguration(exclude = SecurityAutoConfiguration.class)
public class TestMvcApplication
{
}
