/*
 *    Copyright 2019-2023 the original author or authors.
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

package com.proit.app.dict.service.ddl.ast;

import com.proit.app.dict.api.domain.DictFieldType;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum ColumnType
{
	INT("int"),
	DECIMAL("decimal"),
	TEXT("text"),
	BOOL("bool", "boolean"),
	DATE("date"),
	TIMESTAMP("timestamp"),
	ENUM("anything"),
	JSON("json"),
	ATTACHMENT("attachment"),
	GEO_JSON("geo_json");

	private final Set<String> aliases;

	ColumnType(String... aliases)
	{
		this.aliases = Set.of(aliases);
	}

	public static List<String> getAllAliases()
	{
		return Arrays.stream(ColumnType.values()).map(ColumnType::getAliases).flatMap(Collection::stream).collect(Collectors.toList());
	}

	public static ColumnType fromString(String str)
	{
		return Arrays.stream(ColumnType.values())
				.filter(it -> it.getAliases().contains(str.toLowerCase()))
				.findFirst()
				.orElse(ENUM);
	}

	public DictFieldType toDictFieldType()
	{
		return switch (this)
			{
				case INT -> DictFieldType.INTEGER;
				case DECIMAL -> DictFieldType.DECIMAL;
				case TEXT -> DictFieldType.STRING;
				case BOOL -> DictFieldType.BOOLEAN;
				case DATE -> DictFieldType.DATE;
				case TIMESTAMP -> DictFieldType.TIMESTAMP;
				case JSON -> DictFieldType.JSON;
				case ENUM -> DictFieldType.ENUM;
				case ATTACHMENT -> DictFieldType.ATTACHMENT;
				case GEO_JSON -> DictFieldType.GEO_JSON;

				default -> throw new RuntimeException("Отсутствует соответствие типа %s с типом поля справочника.".formatted(this));
			};
	}
}
