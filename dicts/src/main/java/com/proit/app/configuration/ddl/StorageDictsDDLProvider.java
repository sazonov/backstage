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

package com.proit.app.configuration.ddl;

import com.proit.app.configuration.properties.DictsProperties;
import com.proit.app.exception.EngineException;
import com.proit.app.service.backend.Engine;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Провайдер, инициирующий создание {@link com.proit.app.domain.Dict}/{@link com.proit.app.domain.VersionScheme}
 * в целевом адаптере укзанным в {@link DictsProperties#getStorage()} при первом запуске аппликации.
 * Приоритет, должен быть выше чем {@link StorageMigrationDictsDDLProvider}, по причине наличия обьектов
 * {@link com.proit.app.domain.Dict}/{@link com.proit.app.domain.VersionScheme} в источниках данных, в том числе при первой инициализации.
 */
@Component
@RequiredArgsConstructor
@Order(DDLConfiguration.DDL_PRECEDENCE_SYSTEM)
public class StorageDictsDDLProvider implements DictsDDLProvider, BeanNameAware
{
	private final DictsProperties dictsProperties;

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
		var firstInit = engines.stream()
				.allMatch(it -> !it.dictExists() && !it.versionSchemeExists());

		if (firstInit)
		{
			var engine = engines.stream()
					.filter(it -> StringUtils.equalsAny(it.getDictEngine().getName(), dictsProperties.getStorage()))
					.findFirst()
					.orElseThrow(() -> new EngineException("An engine equal to the engine from the storage property doesn't exists."));

			engine.createSchema();
			engine.createDict();
			engine.createVersionScheme();
		}
	}
}
