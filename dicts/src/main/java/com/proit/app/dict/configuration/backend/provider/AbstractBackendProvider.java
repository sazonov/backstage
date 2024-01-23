/*
 *
 *  Copyright 2019-2023 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.proit.app.dict.configuration.backend.provider;

import com.proit.app.dict.configuration.properties.DictsProperties;
import com.proit.app.dict.domain.DictEngine;
import com.proit.app.dict.exception.EngineException;
import com.proit.app.dict.service.backend.Backend;
import com.proit.app.dict.service.backend.Engine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.GenericTypeResolver;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//TODO: написать спеку и тесты на провайдер
@Slf4j
public abstract class AbstractBackendProvider<T extends Backend> implements BackendProvider<T>
{
	private final Map<String, T> backendMap = new HashMap<>();

	private final Class<T> type;

	public AbstractBackendProvider(DictsProperties dictsProperties, List<T> backends)
	{
		this.type = (Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(), AbstractBackendProvider.class);

		var availableEngineNames = dictsProperties.getEngines();

		Stream.of(backends)
				.peek(it -> validateBackends(dictsProperties.getEngines(), it))
				.flatMap(Collection::stream)
				.filter(it -> availableEngineNames.contains(it.getEngine().getDictEngine().getName()))
				.map(it -> Map.of(it.getEngine().getDictEngine().getName(), it))
				.forEach(backendMap::putAll);

		logUnidentifiedEngines(availableEngineNames, backends);
	}

	@Override
	public T getBackendByEngineName(String engineName)
	{
		var backend = backendMap.get(engineName);

		if (backend == null)
		{
			var existedEngineNames = String.join(", ", backendMap.keySet());

			throw new EngineException("Не найдена имплементация %s адаптера с типом Engine '%s' в значении true. Существующие Engine: [%s]"
					.formatted(type.getSimpleName(), engineName, existedEngineNames));
		}

		return backend;
	}

	private void validateBackends(Set<String> propertyEngines, List<T> backends)
	{
		var implementedEngineNames = backends.stream()
				.map(Backend::getEngine)
				.map(Engine::getDictEngine)
				.map(DictEngine::getName)
				.collect(Collectors.toSet());

		propertyEngines.stream()
				.filter(Predicate.not(implementedEngineNames::contains))
				.collect(Collectors.collectingAndThen(Collectors.joining(", "), Optional::of))
				.filter(Predicate.not(String::isEmpty))
				.ifPresent(it -> {
					throw new EngineException("Не найдены имплементации %s адаптеров с типами Engine [%s]"
							.formatted(type.getSimpleName(), it));
				});
	}

	private void logUnidentifiedEngines(Set<String> availableEngines, List<T> backends)
	{
		backends.stream()
				.map(Backend::getEngine)
				.map(Engine::getDictEngine)
				.map(DictEngine::getName)
				.filter(Predicate.not(availableEngines::contains))
				.collect(Collectors.collectingAndThen(Collectors.joining(", "), Optional::of))
				.filter(Predicate.not(String::isEmpty))
				.ifPresent(it -> log.warn(("Найдены имплементации %s адаптеров с типами Engine '[%s]'"
						+ " не указанные в файле свойств '%s'.")
						.formatted(type.getSimpleName(), DictsProperties.ENGINE_PROPERTY, it)));
	}
}
