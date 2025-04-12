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

package com.proit.app.dict.configuration.ddl;

import com.proit.app.dict.configuration.properties.DictsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(value = DictsProperties.DDL_ACTIVATION_PROPERTY, matchIfMissing = true)
@EnableConfigurationProperties(DictsProperties.class)
@DependsOn({"jsonUtils", "dictEngineUpdater"})
public class DictsStorageDDLConfiguration
{
	private final List<DictsStorageDDLProvider> dictsDDLProviders;

	@PostConstruct
	public void initialize()
	{
		dictsDDLProviders.stream()
				.sorted(AnnotationAwareOrderComparator.INSTANCE)
				.peek(it -> log.info("Applying DDL with '{}'...", it.getName()))
				.forEach(DictsStorageDDLProvider::update);
	}
}
