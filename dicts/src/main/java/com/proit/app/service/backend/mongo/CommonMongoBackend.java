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

import com.mongodb.BasicDBObject;
import com.proit.app.repository.mongo.MongoDictRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.OutOperation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class CommonMongoBackend
{
	protected static final String SCHEME_COLLECTION_ID = "dict";

	@Autowired
	protected MongoTemplate mongoTemplate;
	@Autowired
	protected MongoDictRepository mongoDictRepository;

	protected final AtomicBoolean activeTransaction = new AtomicBoolean(false);

	protected TransactionData transactionData;

	@Getter
	protected static class TransactionData
	{
		private final Map<String, String> affectedDictIds = new LinkedHashMap<>();
	}

	protected void addToTransactionData(String dictId, boolean schemeUsed)
	{
		if (activeTransaction.get())
		{
			if (schemeUsed && !transactionData.getAffectedDictIds().containsKey(SCHEME_COLLECTION_ID))
			{
				var dictCopyId = copyDict(SCHEME_COLLECTION_ID);
				transactionData.getAffectedDictIds().put(SCHEME_COLLECTION_ID, dictCopyId);
			}

			if (dictId != null && !transactionData.getAffectedDictIds().containsKey(dictId))
			{
				var dictCopyId = copyDict(dictId);
				transactionData.getAffectedDictIds().put(dictId, dictCopyId);
			}
		}
	}

	private String copyDict(String dictId)
	{
		var copyDictId = dictId + "_clone";
		mongoTemplate.createCollection(copyDictId);

		var outOperation = new OutOperation(copyDictId);
		mongoTemplate.aggregate(Aggregation.newAggregation(outOperation), dictId, BasicDBObject.class);

		return copyDictId;
	}
}
