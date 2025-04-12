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

import com.proit.app.database.configuration.ddl.DDLConfiguration;
import com.proit.app.dict.configuration.properties.DictsProperties;
import com.proit.app.dict.service.backend.DictBackend;
import com.proit.app.dict.service.backend.Engine;
import com.proit.app.dict.service.migration.StorageMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Приоритет провайдера, должен быть ниже чем {@link StorageDictsDDLProvider}
 */
@Component
@RequiredArgsConstructor
@Order(DDLConfiguration.DDL_PRECEDENCE_SYSTEM + 100)
public class StorageMigrationDictsDDLProvider implements DictsStorageDDLProvider, BeanNameAware
{
	private final DictsProperties dictsProperties;

	private final StorageMigrationService storageMigrationService;

	private final DictBackend dictBackend;

	private final List<Engine> engines;

	@Setter
	private String beanName;

	@Override
	public String getName()
	{
		return beanName;
	}

	@Override
	public void update()
	{
		engines.stream()
				.filter(it -> it.dictExists() && it.versionSchemeExists())
				.filter(it -> !StringUtils.equals(it.getDictEngine().getName(), dictsProperties.getStorage()))
				.findFirst()
				.ifPresent(it -> storageMigrationService.migrate(it, dictBackend.getEngine()));
	}
}
