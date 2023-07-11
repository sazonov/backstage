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

import com.mongodb.MongoNamespace;
import com.proit.app.domain.DictIndex;
import com.proit.app.domain.VersionScheme;
import com.proit.app.exception.dictionary.DictException;
import com.proit.app.exception.dictionary.DictNotFoundException;
import com.proit.app.repository.mongo.MongoDictRepository;
import com.proit.app.repository.mongo.MongoVersionSchemeRepository;
import com.proit.app.service.backend.VersionSchemeBackend;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MongoVersionSchemeBackend extends CommonMongoBackend implements VersionSchemeBackend
{
	private final MongoTemplate mongoTemplate;

	private final MongoDictRepository mongoDictRepository;
	private final MongoVersionSchemeRepository mongoVersionSchemeRepository;

	@Override
	public void beginDDL()
	{
		if (dictTransactionProvider.isActiveTransaction())
		{
			throw new DictException("Транзакция DDL уже открыта.");
		}

		if (dictTransactionProvider.isTransactionExpectedAndSet(false, true))
		{
			dictTransactionProvider.transactionDataAsNew();
		}
		else
		{
			throw new DictException("Транзакция DDL уже открыта.");
		}
	}

	@Override
	public void commitDDL()
	{
		if (!dictTransactionProvider.isActiveTransaction())
		{
			throw new DictException("Отсутствует транзакция DDL.");
		}

		dictTransactionProvider.getAffectedDictIds()
				.forEach((original, copied) -> mongoTemplate.dropCollection(copied));

		dictTransactionProvider.transactionDataAsNew();
		dictTransactionProvider.transactionAsInactive();
	}

	@Override
	public void rollbackDDL()
	{
		if (!dictTransactionProvider.isActiveTransaction())
		{
			throw new DictException("Отсутствует транзакция DDL.");
		}

		dictTransactionProvider.getAffectedDictIds().forEach(this::rollbackChanges);

		dictTransactionProvider.transactionDataAsNew();
		dictTransactionProvider.transactionAsInactive();
	}

	@Override
	public List<VersionScheme> findAll()
	{
		return mongoVersionSchemeRepository.findAll();
	}

	@Override
	public VersionScheme create(VersionScheme versionScheme)
	{
		return mongoVersionSchemeRepository.save(versionScheme);
	}

	@Override
	public Optional<VersionScheme> findByScript(String script)
	{
		return mongoVersionSchemeRepository.findByScript(script);
	}

	private void rollbackChanges(String original, String copied)
	{
		mongoTemplate.dropCollection(original);

		if (original.equals(SCHEME_COLLECTION_ID))
		{
			mongoTemplate.getCollection(copied)
					.renameCollection(new MongoNamespace(mongoTemplate.getDb().getName(), original));
		}
		else if (mongoDictRepository.existsById(original))
		{
			mongoTemplate.getCollection(copied)
					.renameCollection(new MongoNamespace(mongoTemplate.getDb().getName(), original));

			mongoDictRepository.findById(original)
					.orElseThrow(() -> new DictNotFoundException(original))
					.getIndexes()
					.forEach(it -> mongoTemplate.indexOps(original).ensureIndex(buildIndex(it)));
		}
		else
		{
			mongoTemplate.dropCollection(copied);
		}
	}

	private Index buildIndex(DictIndex source)
	{
		var target = new Index().named(source.getId());

		source.getFields().forEach(it -> target.on(it, source.getDirection()));

		return target;
	}
}
