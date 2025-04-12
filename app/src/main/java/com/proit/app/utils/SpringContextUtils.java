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

package com.proit.app.utils;

import lombok.experimental.UtilityClass;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Collection;
import java.util.List;

@UtilityClass
public class SpringContextUtils
{
	// Создаёт bean factory в контексте которой по стандартным именам доступны все бины с аннотацией @ConfigurationProperties.
	public BeanFactory exposeConfigurationProperties(List<String> basePackages, BeanFactory beanFactory)
	{
		var configPostProcessor = beanFactory.getBean(ConfigurationPropertiesBindingPostProcessor.class);

		var factory = new DefaultListableBeanFactory(beanFactory);
		factory.addBeanPostProcessor(configPostProcessor);

		var beanNameGenerator = new AnnotationBeanNameGenerator();

		var scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(ConfigurationProperties.class));

		basePackages
				.stream()
				.map(scanner::findCandidateComponents)
				.flatMap(Collection::stream)
				.forEach(beanDefinition -> {
					factory.registerBeanDefinition(beanNameGenerator.generateBeanName(beanDefinition, factory), beanDefinition);
				});

		return factory;
	}
}
