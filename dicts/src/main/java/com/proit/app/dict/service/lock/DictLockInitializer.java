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

package com.proit.app.dict.service.lock;

import com.proit.app.dict.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Должен быть выполнен после {@link com.proit.app.dict.configuration.ddl.DictsStorageDDLConfiguration}, по причине необходимости
 * наличия созданной схемы dict.
 * @see com.proit.app.dict.configuration.ddl.StorageDictsDDLProvider
 * @see com.proit.app.dict.configuration.ddl.StorageMigrationDictsDDLProvider
 */
@Component
@DependsOn("dictsStorageDDLConfiguration")
@RequiredArgsConstructor
public class DictLockInitializer
{
	private final DictService dictService;

	private final DictLockService dictLockService;

	/**
	 * Первоначальное наполнение коллекции локов, чтобы гарантировать их наличие на каждый справочник.
	 * @see DictLockService
	 */
	@PostConstruct
	public void initialize()
	{
		dictService.getAll()
				.forEach(dict -> dictLockService.addLock(dict.getId()));
	}
}

