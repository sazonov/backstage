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

package com.proit.app.dict.service.backend.mongo;

import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.domain.DictIndex;
import com.proit.app.dict.repository.mongo.MongoDictRepository;
import com.proit.app.dict.service.migration.DictTransactionProvider;
import com.proit.app.dict.constant.ServiceFieldConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

public abstract class AbstractMongoBackend
{
	@Autowired
	protected MongoEngine mongoEngine;

	@Autowired
	protected MongoTemplate mongoTemplate;

	@Autowired
	protected MongoDictRepository mongoDictRepository;

	@Autowired
	protected DictTransactionProvider dictTransactionProvider;

	protected void addTransactionData(Dict dict, boolean schemeUsed)
	{
		dictTransactionProvider.addDictTransactionItem(dict, schemeUsed);
	}

	protected void convertMongoServiceFields(Dict dict)
	{
		dict.getFields()
				.stream()
				.filter(it -> it.getId().equals(ServiceFieldConstants._ID))
				.forEach(it -> it.setId(ServiceFieldConstants.ID));
	}

	protected Index buildIndex(DictIndex source)
	{
		var target = new Index().named(source.getId());

		source.getFields().forEach(it -> target.on(it, source.getDirection()));

		return target;
	}
}
