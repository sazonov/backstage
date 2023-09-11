package com.proit.app;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;

@SpringBootConfiguration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan(value = "com.proit",
		excludeFilters = @ComponentScan.Filter(
				type = FilterType.ASSIGNABLE_TYPE,
				value = TestMvcApplication.class))
public class TestApplication
{
}
