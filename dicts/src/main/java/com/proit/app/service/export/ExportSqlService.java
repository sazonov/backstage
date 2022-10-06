/*
 *    Copyright 2019-2022 the original author or authors.
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

package com.proit.app.service.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proit.app.domain.DictItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportSqlService implements ExportService
{
	private static final String SQL_INSERT_TEMPLATE = "INSERT INTO %s (%s) VALUES (%s);";

	private final ObjectMapper objectMapper;

	public byte[] export(String dictId, List<DictItem> items, String userId)
	{
		return items.stream()
				.map(DictItem::getData)
				.map(data -> SQL_INSERT_TEMPLATE.formatted(dictId, mapDictKeys(data), mapDictData(data)))
				.collect(Collectors.joining("\n"))
				.getBytes(StandardCharsets.UTF_8);
	}

	private String mapDictKeys(Map<String, Object> dict)
	{
		return String.join(", ", dict.keySet());
	}

	private String mapDictData(Map<String, Object> dict)
	{
		return dict.values().stream()
				.map(el -> mapElement(el))
				.collect(Collectors.joining(", "));
	}

	private String mapElement(Object el)
	{
		try
		{
			return objectMapper/*.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true)*/
					.writer()
					.writeValueAsString(el)
					.replace("\"", "'");
		}
		catch (JsonProcessingException e)
		{
			return null;
		}
	}
}
