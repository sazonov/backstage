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

package com.proit.app.service.backend.mongo;

import com.proit.app.domain.Dict;
import com.proit.app.domain.DictEngine;
import com.proit.app.domain.VersionScheme;
import com.proit.app.service.backend.Engine;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MongoEngine implements Engine
{
	public static final String MONGO = "mongo";

	private final MongoTemplate mongoTemplate;

	@Override
	public DictEngine getDictEngine()
	{
		return new DictEngine(MONGO);
	}

	@Override
	public void createSchema()
	{
		// ignored
	}

	@Override
	public void createDict()
	{
		mongoTemplate.createCollection(Dict.class);
	}

	@Override
	public void createVersionScheme()
	{
		mongoTemplate.createCollection(VersionScheme.class);
	}

	@Override
	public boolean dictExists()
	{
		return mongoTemplate.collectionExists(Dict.class);
	}

	@Override
	public boolean versionSchemeExists()
	{
		return mongoTemplate.collectionExists(VersionScheme.class);
	}

	@Override
	public void dropDict()
	{
		mongoTemplate.dropCollection(Dict.class);
	}

	@Override
	public void dropVersionScheme()
	{
		mongoTemplate.dropCollection(VersionScheme.class);
	}
}
