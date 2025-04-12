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

package com.proit.app.dict.service.mapping;

import com.proit.app.dict.constant.ServiceFieldConstants;
import com.proit.app.dict.domain.DictFieldName;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class DictFieldNameMappingService
{
	public DictFieldName mapDictFieldName(String selectField)
	{
		var dictFieldName = buildDictFieldName(selectField);

		return withCorrectFieldId(dictFieldName);
	}

	private DictFieldName buildDictFieldName(String selectField)
	{
		Function<String, DictFieldName> withDict = field -> {
			var fieldArray = field.split("\\.");

			return new DictFieldName(fieldArray[0], fieldArray[1]);
		};

		Function<String, DictFieldName> withoutDict = field -> new DictFieldName(null, field);

		return selectField.contains(".") ? withDict.apply(selectField) : withoutDict.apply(selectField);
	}

	@Deprecated(forRemoval = true)
	private DictFieldName withCorrectFieldId(DictFieldName dictFieldName)
	{
		var replacedFieldId = dictFieldName.getFieldId().replaceAll(ServiceFieldConstants._ID, ServiceFieldConstants.ID);

		return new DictFieldName(dictFieldName.getDictId(), replacedFieldId);
	}
}
