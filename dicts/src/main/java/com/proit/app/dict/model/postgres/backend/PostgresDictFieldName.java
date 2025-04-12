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

package com.proit.app.dict.model.postgres.backend;

import lombok.Getter;

@Getter
public class PostgresDictFieldName
{
	private final PostgresWord wordDictId;

	private final PostgresWord wordFieldId;

	private final String wordJoint;

	public PostgresDictFieldName(PostgresWord wordDictId, PostgresWord fieldId)
	{
		this(wordDictId, fieldId, concat(wordDictId.getQuotedIfKeyword(), fieldId.getQuotedIfKeyword()));
	}

	private PostgresDictFieldName(PostgresWord wordDictId, PostgresWord fieldId, String wordJoint)
	{
		this.wordDictId = wordDictId;
		this.wordFieldId = fieldId;
		this.wordJoint = wordJoint;
	}

	private static String concat(String dictId, String fieldId)
	{
		return dictId == null ? fieldId : String.join(".", dictId, fieldId);
	}
}
