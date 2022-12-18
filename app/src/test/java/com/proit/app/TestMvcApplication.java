package com.proit.app;

import com.proit.app.configuration.properties.ApiProperties;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@ComponentScan(basePackages = "com.proit.app.controller")
@Import(ApiProperties.class)
@SpringBootConfiguration
public class TestMvcApplication
{
}
