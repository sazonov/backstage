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

package com.proit.app.dict.updater;

import com.proit.app.dict.configuration.backend.provider.DictBackendProvider;
import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.exception.EngineException;
import com.proit.app.dict.service.backend.DictSchemeBackend;
import com.proit.app.dict.service.backend.Engine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DictEngineUpdater
{
	private final List<Engine> engines;

	private final List<DictSchemeBackend> schemeBackends;

	private final DictBackendProvider dictBackendProvider;

	@PostConstruct
	public void setupDictEnginesIfNull()
	{
		engines.stream()
				.filter(it -> it.dictExists() && it.versionSchemeExists())
				.findFirst()
				.ifPresent(this::setupEngine);
	}

	private void setupEngine(Engine it)
	{
		var dictBackend = dictBackendProvider.getBackendByEngineName(it.getDictEngine().getName());

		dictBackend.getAllDicts()
				.stream()
				.filter(dict -> dict.getEngine() == null)
				.peek(this::setup)
				.map(dictBackend::updateDict)
				.forEach(dict -> log.warn("Для справочника '{}', установлен local engine '{}'.",
						dict.getId(), dict.getEngine()));
	}

	private void setup(Dict dict)
	{
		var current = schemeBackends.stream()
				.filter(it -> it.existsDictSchemeById(dict.getId()))
				.findFirst()
				.orElseThrow(() -> new EngineException("The dict '%s' doesn't exists in all engines"
						.formatted(dict.getId())))
				.getEngine()
				.getDictEngine();

		dict.setEngine(current);
	}
}
