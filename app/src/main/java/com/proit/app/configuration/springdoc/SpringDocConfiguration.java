/*
 *    Copyright 2019-2022 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.proit.app.configuration.springdoc;

import com.proit.app.configuration.properties.ApiProperties;
import com.proit.app.configuration.properties.AppProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.SpringDocUtils;
import org.springdoc.core.SwaggerUiConfigProperties;
import org.springdoc.core.converters.PageableOpenAPIConverter;
import org.springdoc.core.converters.SortOpenAPIConverter;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
@ConditionalOnProperty(name = ApiProperties.SWAGGER_ACTIVATION_PROPERTY, matchIfMissing = true)
@RequiredArgsConstructor
public class SpringDocConfiguration implements BeanPostProcessor
{
	private final AppProperties appProperties;
	private final ApiProperties apiProperties;

	@Bean
	ObjectMapperProvider springDocObjectMapperProvider(SpringDocConfigProperties springDocConfigProperties)
	{
		return new ObjectMapperProvider(springDocConfigProperties);
	}

	@Bean
	public PageableOpenAPIConverter pageableOpenAPIConverter(ObjectMapperProvider objectMapperProvider)
	{
		SpringDocUtils.getConfig()
				.replaceParameterObjectWithClass(org.springframework.data.domain.Pageable.class, com.proit.app.configuration.springdoc.models.Pageable.class)
				.replaceParameterObjectWithClass(org.springframework.data.domain.PageRequest.class, com.proit.app.configuration.springdoc.models.Pageable.class);

		return new PageableOpenAPIConverter(objectMapperProvider);
	}

	@Bean
	public SortOpenAPIConverter sortOpenAPIConverter(ObjectMapperProvider objectMapperProvider)
	{
		SpringDocUtils.getConfig()
				.replaceParameterObjectWithClass(org.springframework.data.domain.Sort.class, com.proit.app.configuration.springdoc.models.Sort.class);

		return new SortOpenAPIConverter(objectMapperProvider);
	}

	@Bean
	public OpenAPI apiDocumentation()
	{
		return new OpenAPI()
				.info(new Info()
						.title(appProperties.getModule())
						.version(appProperties.getVersion()));
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
	{
		if (bean instanceof SpringDocConfigProperties springDocConfigProperties)
		{
			Optional.ofNullable(apiProperties.getSwagger())
					.ifPresent(swaggerProperties -> {
						Optional.ofNullable(swaggerProperties.getPackagesToScan())
								.ifPresent(springDocConfigProperties::setPackagesToScan);

						Optional.ofNullable(swaggerProperties.getPathsToMatch())
								.ifPresent(springDocConfigProperties::setPathsToMatch);
					});
		}
		else if (bean instanceof SwaggerUiConfigProperties swaggerUiConfigProperties)
		{
			swaggerUiConfigProperties.setDocExpansion("none");
			swaggerUiConfigProperties.setTagsSorter("alpha");

			Optional.ofNullable(apiProperties.getSwagger())
					.ifPresent(swaggerProperties -> swaggerUiConfigProperties.getCsrf().setEnabled(swaggerProperties.isCsrf()));
		}

		return bean;
	}
}
