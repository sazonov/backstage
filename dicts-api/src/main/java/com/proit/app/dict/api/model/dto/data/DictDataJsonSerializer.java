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

package com.proit.app.dict.api.model.dto.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.proit.app.model.json.AbstractCustomJsonSerializer;

import java.util.Map;

public class DictDataJsonSerializer extends AbstractCustomJsonSerializer<Map<String, Object>>
{
	@Override
	public void serializeValue(Map<String, Object> value, JsonGenerator gen)
	{
		value.forEach((field, object) -> writeNodeWithTypePrefix(gen, field, object));
	}
}
