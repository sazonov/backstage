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

package com.proit.app.dict.service.migration;

import com.proit.app.dict.configuration.backend.provider.DictBackendProvider;
import com.proit.app.dict.configuration.backend.provider.VersionSchemeBackendProvider;
import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.domain.VersionScheme;
import com.proit.app.dict.service.backend.DictBackend;
import com.proit.app.dict.service.backend.Engine;
import com.proit.app.dict.service.backend.VersionSchemeBackend;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageMigrationService
{
	private final DictBackendProvider dictBackendProvider;
	private final VersionSchemeBackendProvider schemeBackendProvider;

	private final DictBackend targetDictBackend;
	private final VersionSchemeBackend targetSchemeBackend;

	public void migrate(Engine sourceEngine, Engine targetEngine)
	{
		var sourceEngineName = sourceEngine.getDictEngine().getName();
		var targetEngineName = targetEngine.getDictEngine().getName();

		log.warn("Старт миграции {}, {} из '{}' в '{}'.", Dict.class.getSimpleName(),
				VersionScheme.class.getSimpleName(), sourceEngineName, targetEngineName);

		createTarget(targetEngine);

		transferData(sourceEngineName);

		dropSource(sourceEngine);

		log.warn("Миграция {}, {} в '{}' успешно завершена.", Dict.class.getSimpleName(),
				VersionScheme.class.getSimpleName(), targetEngineName);
	}

	private void createTarget(Engine targetEngine)
	{
		targetEngine.createSchema();
		targetEngine.createDict();
		targetEngine.createVersionScheme();
	}

	private void transferData(String sourceEngineName)
	{
		var sourceDictBackend = dictBackendProvider.getBackendByEngineName(sourceEngineName);
		var sourceVersionSchemeBackend = schemeBackendProvider.getBackendByEngineName(sourceEngineName);

		var sourceDicts = sourceDictBackend.getAllDicts();
		var sourceSchemes = sourceVersionSchemeBackend.findAll();

		sourceDicts.forEach(targetDictBackend::saveDict);
		sourceSchemes.forEach(targetSchemeBackend::saveVersionScheme);
	}

	private void dropSource(Engine sourceEngine)
	{
		sourceEngine.dropDict();
		sourceEngine.dropVersionScheme();
	}
}
