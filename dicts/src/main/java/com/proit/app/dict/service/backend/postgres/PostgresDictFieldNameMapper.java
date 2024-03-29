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

package com.proit.app.dict.service.backend.postgres;

import com.proit.app.dict.domain.DictFieldName;
import com.proit.app.dict.model.postgres.backend.PostgresDictFieldName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PostgresDictFieldNameMapper
{
	private final PostgresReservedKeyword reservedKeyword;

	public PostgresDictFieldName mapFrom(String dictId, DictFieldName fieldName)
	{
		return postgresDictFieldName(dictId, fieldName);
	}

	public List<PostgresDictFieldName> mapFrom(String dictId, List<DictFieldName> fieldNames)
	{
		return fieldNames.stream()
				.map(it -> mapFrom(dictId, it))
				.toList();
	}

	private PostgresDictFieldName postgresDictFieldName(String dictId, DictFieldName fieldName)
	{
		var wordDictId = Optional.ofNullable(fieldName.getDictId())
				.map(reservedKeyword::postgresWordMap)
				.map(it -> it.get(fieldName.getDictId()))
				.orElse(reservedKeyword.postgresWordMap(dictId).get(dictId));

		var wordFieldId = reservedKeyword.postgresWordMap(fieldName.getFieldId()).get(fieldName.getFieldId());

		return new PostgresDictFieldName(wordDictId, wordFieldId);
	}
}
