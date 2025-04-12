package com.proit.app.api;

import com.proit.app.api.configuration.openapi.MultipleOpenApiSupportConfiguration;
import com.proit.app.api.configuration.openapi.SpringDocConfigProperties;
import com.proit.app.api.configuration.openapi.SpringDocConfiguration;
import com.proit.app.api.configuration.openapi.SpringDocWebMvcConfiguration;
import com.proit.app.api.configuration.properties.ApiProperties;
import com.proit.app.configuration.properties.AppProperties;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Import;

@Import({ApiProperties.class, AppProperties.class, MultipleOpenApiSupportConfiguration.class, SpringDocConfiguration.class, SpringDocConfigProperties.class, SpringDocWebMvcConfiguration.class})
@SpringBootConfiguration
@ImportAutoConfiguration(exclude = SecurityAutoConfiguration.class)
public class TestSwaggerApplication
{
}
