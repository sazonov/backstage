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

package com.proit.app.model.postgres.backend;

import com.proit.app.utils.JsonUtils;
import com.proit.app.utils.StreamCollectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static com.proit.app.model.dictitem.DictItemColumnName.*;

@AllArgsConstructor
@Getter
public class PostgresDictItem
{
	private final String dictId;

	private final String id;

	private final Map<String, Object> dictData;

	private final List<Map<String, Object>> history;

	private final Long version;

	private final Timestamp created;

	private final Timestamp updated;

	private final Timestamp deleted;

	private final String deletionReason;

	public PostgresDictItem(String dictId, Map<String, Object> postgresDictData)
	{
		this.dictId = dictId;
		this.id = (String) postgresDictData.get(placeholder(dictId, ID.getName()));
		this.version = (Long) postgresDictData.get(placeholder(dictId, VERSION.getName()));

		this.dictData = postgresDictData.entrySet()
				.stream()
				.collect(StreamCollectors.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));

		this.history = postgresDictData.get(placeholder(dictId, HISTORY.getName())) == null
				? null : JsonUtils.attributeValueMaps(postgresDictData.get(placeholder(dictId, HISTORY.getName())));

		this.created = (Timestamp) postgresDictData.get(placeholder(dictId, CREATED.getName()));
		this.updated = (Timestamp) postgresDictData.get(placeholder(dictId, UPDATED.getName()));
		this.deleted = (Timestamp) postgresDictData.get(placeholder(dictId, DELETED.getName()));
		this.deletionReason = (String) postgresDictData.get(placeholder(dictId, DELETION_REASON.getName()));
	}

	private String placeholder(String dictId, String column)
	{
		return String.join("__", dictId.toLowerCase(), column);
	}
}
