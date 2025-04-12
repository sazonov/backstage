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

package com.proit.app.database.configuration.ddl;

import com.proit.app.database.configuration.properties.DDLProperties;
import com.proit.app.utils.MapUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import javax.annotation.PostConstruct;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(DDLProperties.class)
public class DDLConfiguration
{
	/**
	 * Приоритет DDL провайдеров для системных компонентов.
	 */
	public static final int DDL_PRECEDENCE_SYSTEM = 100;

	/**
	 * Приоритет DDL провайдеров для компонентов приложения.
	 */
	public static final int DDL_PRECEDENCE_APP = 1000;

	private final Map<String, DDLProvider> ddlProviders;

	@PostConstruct
	public void initialize()
	{
		ddlProviders.values()
				.stream()
				.sorted(AnnotationAwareOrderComparator.INSTANCE)
				.peek(provider -> log.info("Applying DDL with '{}'...", MapUtils.getKeyByValue(ddlProviders, provider)))
				.forEach(DDLProvider::update);
	}
}
