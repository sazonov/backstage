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

package com.proit.app.dict.conversion.dto;

import com.proit.app.conversion.dto.AbstractConverter;
import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.domain.DictEngine;
import com.proit.app.dict.api.model.dto.request.CreateDictRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DictRequestConverter extends AbstractConverter<CreateDictRequest, Dict>
{
	private final DictEnumRequestConverter dictEnumRequestConverter;
	private final DictFieldRequestConverter dictFieldRequestConverter;

	@Override
	public Dict convert(CreateDictRequest source)
	{
		var engine = Optional.ofNullable(source.getEngine())
				.map(DictEngine::new)
				.orElse(null);

		return Dict.builder()
				.id(source.getId())
				.name(source.getName())
				.fields(dictFieldRequestConverter.convert(source.getFields()))
				.enums(dictEnumRequestConverter.convert(source.getEnums()))
				.viewPermission(source.getViewPermission())
				.editPermission(source.getEditPermission())
				.engine(engine)
				.build();
	}
}
