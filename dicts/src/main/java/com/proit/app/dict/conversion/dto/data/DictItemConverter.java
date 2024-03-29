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

package com.proit.app.dict.conversion.dto.data;

import com.proit.app.conversion.dto.AbstractConverter;
import com.proit.app.dict.domain.DictItem;
import com.proit.app.dict.api.model.dto.data.DictItemDto;
import com.proit.app.utils.StreamCollectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DictItemConverter extends AbstractConverter<DictItem, DictItemDto>
{
	@Override
	public DictItemDto convert(DictItem source)
	{
		var data = source.getData()
				.entrySet()
				.stream()
				.peek(field -> {
					if (field.getValue() instanceof DictItem refField)
					{
						var convertedItem = convert(refField);

						field.setValue(convertedItem);
					}
				})
				.collect(StreamCollectors.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));

		return DictItemDto.builder()
				.id(source.getId())
				.data(data)
				.history(source.getHistory())
				.version(source.getVersion())
				.created(source.getCreated())
				.updated(source.getUpdated())
				.deleted(source.getDeleted())
				.deletionReason(source.getDeletionReason())
				.build();
	}
}
