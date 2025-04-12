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

package com.proit.app.dict.conversion.dto.data;

import com.proit.app.conversion.dto.AbstractConfigurableConverter;
import com.proit.app.dict.api.model.dto.data.DictItemDto;
import com.proit.app.dict.domain.DictItem;
import com.proit.app.utils.StreamCollectors;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DictItemConverter extends AbstractConfigurableConverter<DictItem, DictItemDto, DictItemConverter.Configuration>
{
	@Getter
	@Builder
	public static class Configuration
	{
		Class<? extends DictItemDto> targetClass;
	}

	private final Configuration DEFAULT_CONFIGURATION = Configuration.builder()
			.targetClass(DictItemDto.class)
			.build();

	@SneakyThrows
	@Override
	public DictItemDto convert(DictItem source, Configuration configuration)
	{
		var data = source.getData()
				.entrySet()
				.stream()
				.peek(field -> {
					if (field.getValue() instanceof DictItem refField)
					{
						var convertedItem = convert(refField, configuration);

						field.setValue(convertedItem);
					}
				})
				.collect(StreamCollectors.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));

		var target = configuration.targetClass.getDeclaredConstructor().newInstance();

		target.setId(source.getId());
		target.setData(data);
		target.setHistory(source.getHistory());
		target.setVersion(source.getVersion());
		target.setCreated(source.getCreated());
		target.setUpdated(source.getUpdated());
		target.setDeleted(source.getDeleted());
		target.setDeletionReason(source.getDeletionReason());

		return target;
	}

	@Override
	public Configuration configure()
	{
		return DEFAULT_CONFIGURATION;
	}
}
