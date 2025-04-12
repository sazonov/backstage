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

import com.proit.app.dict.configuration.backend.provider.DictDataBackendProvider;
import com.proit.app.dict.configuration.backend.provider.DictSchemeBackendProvider;
import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.domain.DictEngine;
import com.proit.app.dict.domain.DictFieldName;
import com.proit.app.dict.exception.dict.DictStorageMigrationException;
import com.proit.app.dict.service.query.ast.Empty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class DictStorageMigrationService
{
	private final DictDataBackendProvider dictDataBackendProvider;
	private final DictSchemeBackendProvider dictSchemeBackendProvider;

	public DictStorageMigrationService(@Lazy DictDataBackendProvider dictDataBackendProvider,
	                                   DictSchemeBackendProvider dictSchemeBackendProvider)
	{
		this.dictDataBackendProvider = dictDataBackendProvider;
		this.dictSchemeBackendProvider = dictSchemeBackendProvider;
	}

	public void migrate(Dict dict, DictEngine sourceEngine, DictEngine targetEngine)
	{
		Objects.requireNonNull(dict.getId(), "dictId не может быть null.");

		var sourceEngineName = sourceEngine.getName();
		var targetEngineName = targetEngine.getName();

		if (sourceEngineName.equals(targetEngineName))
		{
			throw new DictStorageMigrationException(dict.getId(), "Миграция справочника невозможна для одного и того же engine.");
		}

		try
		{
			log.info("Старт миграции справочника {} из '{}' в '{}'.", dict.getId(), sourceEngineName, targetEngineName);

			createTargetSchema(dict, targetEngineName);

			transferData(dict, sourceEngineName, targetEngineName);

			dropSourceSchema(dict, sourceEngineName);

			log.info("Миграция справочника {} в '{}' успешно завершена.", dict.getId(), targetEngineName);
		}
		catch (Exception e)
		{
			log.error("При миграции справочника {} в '{}' произошла ошибка.", dict.getId(), targetEngineName, e);

			dropTargetSchema(dict, targetEngineName);

			throw new RuntimeException(e);
		}
	}

	private void createTargetSchema(Dict dict, String targetEngineName)
	{
		dictSchemeBackendProvider.getBackendByEngineName(targetEngineName)
				.createDictScheme(dict);
	}

	private void transferData(Dict dict, String sourceEngineName, String targetEngineName)
	{
		var targetDictDataBackend = dictDataBackendProvider.getBackendByEngineName(targetEngineName);

		dictDataBackendProvider.getBackendByEngineName(sourceEngineName)
				.getByFilter(dict, List.of(new DictFieldName(null, "*")), new Empty(), Pageable.unpaged())
				.getContent()
				.forEach(dictItem -> targetDictDataBackend.create(dict, dictItem));
	}

	private void dropSourceSchema(Dict dict, String sourceEngineName)
	{
		dictSchemeBackendProvider.getBackendByEngineName(sourceEngineName)
				.deleteDictSchemeById(dict.getId());
	}

	private void dropTargetSchema(Dict dict, String targetEngineName)
	{
		dictSchemeBackendProvider.getBackendByEngineName(targetEngineName)
				.deleteDictSchemeById(dict.getId());
	}
}
