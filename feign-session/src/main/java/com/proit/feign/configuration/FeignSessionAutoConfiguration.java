package com.proit.feign.configuration;

import com.proit.feign.properties.FeignSessionProperties;
import com.proit.feign.provider.DefaultPrincipalProvider;
import com.proit.feign.provider.PrincipalProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(FeignSessionProperties.class)
public class FeignSessionAutoConfiguration
{
	@Bean
	@ConditionalOnMissingBean(PrincipalProvider.class)
	public DefaultPrincipalProvider defaultPrincipalProvider()
	{
		return new DefaultPrincipalProvider();
	}
}
