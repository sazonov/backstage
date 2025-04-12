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

package com.proit.app.dict.service.backend.mongo;

import com.proit.app.dict.domain.VersionScheme;
import com.proit.app.dict.repository.mongo.MongoVersionSchemeRepository;
import com.proit.app.dict.service.backend.Engine;
import com.proit.app.dict.service.backend.VersionSchemeBackend;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MongoVersionSchemeBackend extends AbstractMongoBackend implements VersionSchemeBackend
{
	private final MongoVersionSchemeRepository mongoVersionSchemeRepository;

	@Override
	public Engine getEngine()
	{
		return mongoEngine;
	}

	@Override
	public List<VersionScheme> findAll()
	{
		return mongoVersionSchemeRepository.findAll();
	}

	@Override
	public VersionScheme saveVersionScheme(VersionScheme versionScheme)
	{
		return mongoVersionSchemeRepository.save(versionScheme);
	}

	@Override
	public Optional<VersionScheme> findByScript(String script)
	{
		return mongoVersionSchemeRepository.findByScript(script);
	}
}
