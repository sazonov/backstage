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

package com.proit.app.model.dto.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.proit.app.model.json.AbstractCustomJsonDeserializer;

import java.util.*;

public class DictDataDeserializer extends AbstractCustomJsonDeserializer<Map<String, Object>>
{
	public Map<String, Object> extractedValue(JsonNode json)
	{
		var result = new HashMap<String, Object>();

		json.fields()
				.forEachRemaining(it -> result.put(it.getKey(), extractValueByFieldName(it.getKey())));

		return result;
	}
}
