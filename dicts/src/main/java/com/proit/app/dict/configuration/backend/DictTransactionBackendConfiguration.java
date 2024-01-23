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
import com.proit.app.dict.exception.EngineException;
import com.proit.app.dict.service.backend.DictTransactionBackend;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Optional;

@Configuration
@EnableConfigurationProperties(DictsProperties.class)
public class DictTransactionBackendConfiguration extends AbstractDictConfiguration<DictTransactionBackend>
{
	public DictTransactionBackendConfiguration(DictsProperties dictsProperties, List<DictTransactionBackend> backends)
	{
		super(dictsProperties, backends);
	}

	@Primary
	@Bean(name = "dictTransactionBackend")
	protected DictTransactionBackend primaryBackend()
	{
		return Optional.ofNullable(backendMap.get(dictsProperties.getStorage()))
				.orElseThrow(() -> new EngineException("При конфигурировании %s произошла ошибка."
						.formatted(type.getSimpleName())));
	}
}