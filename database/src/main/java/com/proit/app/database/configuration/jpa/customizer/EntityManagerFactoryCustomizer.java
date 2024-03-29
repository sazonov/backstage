/*
 *    Copyright 2019-2023 the original author or authors.
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

import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;

import java.util.Collections;
import java.util.Set;

public interface EntityManagerFactoryCustomizer extends PersistenceUnitPostProcessor
{
	default Set<String> getPackagesToScan()
	{
		return Collections.emptySet();
	}

	default Set<String> getMappingResources()
	{
		return Collections.emptySet();
	}

	default void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo persistenceUnitInfo)
	{
	}
}
