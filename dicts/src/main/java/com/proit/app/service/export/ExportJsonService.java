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

package com.proit.app.service.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proit.app.conversion.dto.DictConverter;
import com.proit.app.conversion.dto.data.DictItemConverter;
import com.proit.app.domain.DictItem;
import com.proit.app.exception.AppException;
import com.proit.app.model.api.ApiStatusCodeImpl;
import com.proit.app.model.dto.ExportedDictDto;
import com.proit.app.service.DictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportJsonService implements ExportService
{
//TODO: при реализации экспорта/импорта необходима избавиться от конвертера на уровне сервисов
	private final DictConverter dictConverter;
	private final DictItemConverter dictItemConverter;

	private static final int DEFAULT_EXPORT_FORMAT_VERSION = 1;

	private final ObjectMapper objectMapper;

	private final DictService dictService;

	public byte[] export(String dictId, List<DictItem> items, String userId)
	{
		try
		{
			return objectMapper
					.writer()
					.writeValueAsBytes(new ExportedDictDto(
							DEFAULT_EXPORT_FORMAT_VERSION, dictConverter.convert(dictService.getById(dictId)),
							dictItemConverter.convert(items)));
		}
		catch (Exception e)
		{
			throw new AppException(ApiStatusCodeImpl.DICTS_ERROR, "При экспорте справочника произошла ошибка.", e);
		}
	}
}

