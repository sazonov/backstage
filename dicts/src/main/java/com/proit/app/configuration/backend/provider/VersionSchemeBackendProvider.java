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

package com.proit.app.configuration.backend.provider;

import com.proit.app.configuration.backend.VersionSchemeBackendConfiguration;
import com.proit.app.service.backend.VersionSchemeBackend;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VersionSchemeBackendProvider implements BackendProvider<VersionSchemeBackend>
{
	private final VersionSchemeBackendConfiguration schemeBackendConfiguration;

	@Override
	public VersionSchemeBackend getBackendByEngineName(String engineName)
	{
		return schemeBackendConfiguration.getBackendByEngineName(engineName);
	}
}