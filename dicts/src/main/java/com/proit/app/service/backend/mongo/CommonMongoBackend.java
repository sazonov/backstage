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
import com.proit.app.service.backend.DictTransactionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.OutOperation;

public abstract class CommonMongoBackend
{
	protected static final String SCHEME_COLLECTION_ID = "dict";

	@Autowired protected MongoTemplate mongoTemplate;
	@Autowired protected MongoDictRepository mongoDictRepository;

	@Autowired protected DictTransactionProvider dictTransactionProvider;

	protected void addToTransactionData(String dictId, boolean schemeUsed)
	{
		if (dictTransactionProvider.isActiveTransaction())
		{
			if (schemeUsed && !dictTransactionProvider.getAffectedDictIds().containsKey(SCHEME_COLLECTION_ID))
			{
				var dictCopyId = copyDict(SCHEME_COLLECTION_ID);
				dictTransactionProvider.addAffectedDictId(SCHEME_COLLECTION_ID, dictCopyId);
			}

			if (dictId != null && !dictTransactionProvider.getAffectedDictIds().containsKey(dictId))
			{
				var dictCopyId = copyDict(dictId);
				dictTransactionProvider.addAffectedDictId(dictId, dictCopyId);
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
