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

package com.proit.app.database.configuration.jpa.customizer;

import com.proit.app.configuration.properties.AppProperties;
import com.proit.app.database.configuration.conditional.ConditionalOnJpa;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnJpa
@RequiredArgsConstructor
public class DefaultEntityManagerFactoryCustomizer implements EntityManagerFactoryCustomizer
{
	private final AppProperties appProperties;

	@Override
	public Set<String> getPackagesToScan()
	{
		return new HashSet<>(appProperties.getBasePackages());
	}
}
