/*
 *    Copyright 2019-2024 the original author or authors.
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

package com.proit.app.database.configuration.jpa.eclipselink;

import com.proit.app.configuration.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import javax.annotation.PostConstruct;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class MetaModelVerifier
{

	private final AppProperties appProperties;

	@PostConstruct
	public void initialize()
	{
		var scanner = new ClassPathScanningCandidateComponentProvider(false) {
			@Override
			protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition)
			{
				AnnotationMetadata metadata = beanDefinition.getMetadata();

				return (metadata.isIndependent() && (metadata.isConcrete() || metadata.isAbstract()));
			}
		};
		scanner.addIncludeFilter(new AnnotationTypeFilter(StaticMetamodel.class));

		var models = appProperties.getBasePackages()
				.stream()
				.map(scanner::findCandidateComponents)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());

		log.info("JPA meta models found: {}.", models.size());

		models.forEach(beanDefinition -> {
			try
			{
				Arrays.stream(Class.forName(beanDefinition.getBeanClassName()).getDeclaredFields()).forEach(this::checkField);
			}
			catch (Exception e)
			{
				throw new RuntimeException(String.format("failed to validate JPA meta model: %s", beanDefinition.getBeanClassName()), e);
			}
		});
	}

	private void checkField(Field field)
	{
		try
		{
			if (Attribute.class.isAssignableFrom(field.getType()))
			{
				((Attribute<?, ?>) field.get(null)).getDeclaringType();
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(String.format("failed to validate model field: %s", field.getName()), e);
		}
	}
}
