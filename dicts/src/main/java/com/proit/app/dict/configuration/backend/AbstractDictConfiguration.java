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

package com.proit.app.dict.configuration.backend;

import com.proit.app.dict.configuration.properties.DictsProperties;
import com.proit.app.dict.domain.DictEngine;
import com.proit.app.dict.exception.EngineException;
import com.proit.app.dict.service.backend.Backend;
import com.proit.app.dict.service.backend.Engine;
import org.springframework.core.GenericTypeResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractDictConfiguration<T extends Backend>
{
	protected final Map<String, T> backendMap = new HashMap<>();

	protected final DictsProperties dictsProperties;

	protected final Class<T> type;

	public AbstractDictConfiguration(DictsProperties dictsProperties, List<T> backends)
	{
		this.type = (Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(), AbstractDictConfiguration.class);

		validateBackends(dictsProperties, backends);

		var backendMap = backends.stream()
				.collect(Collectors.toMap(it ->
						it.getEngine().getDictEngine().getName(),
						Function.identity()));

		this.dictsProperties = dictsProperties;

		this.backendMap.putAll(backendMap);
	}

	public T getBackendByEngineName(String engineName)
	{
		validateEngineName(engineName);

		return backendMap.get(engineName);
	}

	protected abstract T primaryBackend();

	private void validateEngineName(String engineName)
	{
		if (!backendMap.containsKey(engineName))
		{
			var existedEngineName = String.join(", ", backendMap.keySet());

			throw new EngineException("Не найдена имплементация %s адаптера с наименованием engine '%s'. Существующие: [%s]"
					.formatted(type.getSimpleName(), engineName, existedEngineName));
		}
	}

	private void validateBackends(DictsProperties dictsProperties, List<T> backends)
	{
		backends.stream()
				.map(Backend::getEngine)
				.map(Engine::getDictEngine)
				.map(DictEngine::getName)
				.filter(dictsProperties.getStorage()::equals)
				.findFirst()
				.orElseThrow(() -> new EngineException("Не существующее в имплементациях %s наименование storage: '%s'"
						.formatted(type.getSimpleName(), dictsProperties.getStorage())));
	}
}
