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

package com.proit.app.dict.service.backend;

import com.proit.app.dict.domain.VersionScheme;

import java.util.List;
import java.util.Optional;

public interface VersionSchemeBackend extends Backend
{
	List<VersionScheme> findAll();

	VersionScheme saveVersionScheme(VersionScheme versionScheme);

	Optional<VersionScheme> findByScript(String script);
}
