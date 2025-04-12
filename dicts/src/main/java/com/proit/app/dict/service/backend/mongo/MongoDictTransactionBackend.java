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

import com.mongodb.BasicDBObject;
import com.proit.app.dict.model.dict.DictTransactionItem;
import com.proit.app.dict.service.backend.DictBackend;
import com.proit.app.dict.service.backend.DictTransactionBackend;
import com.proit.app.dict.service.backend.Engine;
import com.proit.app.dict.service.migration.DictTransactionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.OutOperation;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MongoDictTransactionBackend extends AbstractMongoBackend implements DictTransactionBackend
{
	private final DictBackend dictBackend;
	private final MongoDictSchemeBackend mongoSchemeBackend;

	private final MongoTemplate mongoTemplate;

	public Engine getEngine()
	{
		return mongoEngine;
	}

	public String copyDict(String dictId)
	{
		var copyDictId = dictId + "_clone";
		mongoTemplate.createCollection(copyDictId);

		var outOperation = new OutOperation(copyDictId);
		mongoTemplate.aggregate(Aggregation.newAggregation(outOperation), dictId, BasicDBObject.class);

		return copyDictId;
	}

	public void rollbackChanges(DictTransactionItem transactionItem)
	{
		var original = transactionItem.getOriginalDictId();
		var copy = transactionItem.getCopiedDictId();

		mongoSchemeBackend.deleteDictSchemeById(original);

		if (original.equals(DictTransactionProvider.DICT_SCHEME_ID))
		{
			mongoSchemeBackend.renameDictSchemeById(copy, original);
		}
		else if (dictBackend.existsById(original))
		{
			mongoSchemeBackend.renameDictSchemeById(copy, original);

			dictBackend.getDictById(original)
					.getIndexes()
					.forEach(it -> mongoTemplate.indexOps(original).ensureIndex(buildIndex(it)));
		}
		else
		{
			mongoSchemeBackend.deleteDictSchemeById(copy);
		}
	}
}
