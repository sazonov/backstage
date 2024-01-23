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

package com.proit.app.dict.service.ddl.ast.expression.table.operation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum TableParameter
{
	READ_PERMISSION("readPermission"),
	WRITE_PERMISSION("writePermission");

	private final String alias;

	public static List<String> getAllAliases()
	{
		return Arrays.stream(TableParameter.values()).map(TableParameter::getAlias).collect(Collectors.toList());
	}

	public static TableParameter fromString(String str)
	{
		return Arrays.stream(TableParameter.values()).filter(it -> it.getAlias().equalsIgnoreCase(str))
				.findFirst().orElseThrow(() -> new RuntimeException("Неизвестный параметр справочника: %s. Доступные параметры: %s.".formatted(str, getAllAliases())));
	}
}
