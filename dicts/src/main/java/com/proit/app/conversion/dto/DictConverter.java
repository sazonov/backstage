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

package com.proit.app.conversion.dto;

import com.proit.app.domain.Dict;
import com.proit.app.model.dto.DictDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DictConverter extends AbstractConverter<Dict, DictDto>
{
	private final DictEnumConverter dictEnumConverter;
	private final DictFieldConverter dictFieldConverter;

	@Override
	public DictDto convert(Dict source)
	{
		return DictDto.builder()
				.id(source.getId())
				.name(source.getName())
				.fields(dictFieldConverter.convert(source.getFields()))
				.enums(dictEnumConverter.convert(source.getEnums()))
				.viewPermission(source.getViewPermission())
				.editPermission(source.getEditPermission())
				.deleted(source.getDeleted())
				.build();
	}
}
