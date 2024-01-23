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

package com.proit.app.dict.model.postgres.query;

import com.proit.app.dict.model.postgres.backend.PostgresWord;
import com.proit.app.dict.service.backend.postgres.PostgresReservedKeyword;
import lombok.Getter;

@Getter
public class PostgresQueryField
{
	private final PostgresWord dictId;

	private final PostgresWord fieldId;

	private final String joint;

	public PostgresQueryField(String dictId, String fieldId, PostgresReservedKeyword reservedKeyword)
	{
		this.dictId = dictId(dictId, reservedKeyword);
		this.fieldId = fieldId(fieldId, reservedKeyword);
		this.joint = concat(this.dictId.getQuotedIfKeyword(), this.fieldId.getQuotedIfKeyword());
	}

	private PostgresWord dictId(String dictId, PostgresReservedKeyword reservedKeyword)
	{
		return reservedKeyword.postgresWordMap(dictId).get(dictId);
	}

	private PostgresWord fieldId(String fieldId, PostgresReservedKeyword reservedKeyword)
	{
		return reservedKeyword.postgresWordMap(fieldId).get(fieldId);
	}

	private String concat(String dictId, String fieldId)
	{
		return dictId == null ? fieldId : String.join(".", dictId, fieldId);
	}
}
