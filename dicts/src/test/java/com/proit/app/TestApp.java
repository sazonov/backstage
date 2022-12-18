package com.proit.app;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootConfiguration
@ComponentScan(value = "com.proit",
		excludeFilters = @ComponentScan.Filter(
				type = FilterType.ASPECTJ,
				pattern = "com.proit.TestMvcApplication"))
public class TestApp
{
}
