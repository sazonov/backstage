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

package com.proit.app.service.imp;

import com.proit.app.constant.ServiceFieldConstants;
import com.proit.app.domain.DictItem;
import com.proit.app.model.dictitem.DictDataItem;
import com.proit.app.service.DictDataService;
import com.proit.app.service.DictPermissionService;
import com.proit.app.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportCsvService implements ImportService
{
	private final DictDataService dictDataService;
	private final DictPermissionService dictPermissionService;

	public List<DictItem> importDict(String dictId, InputStream inputStream)
	{
		return importDict(dictId, inputStream, SecurityUtils.getCurrentUserId());
	}

	public List<DictItem> importDict(String dictId, InputStream inputStream, String userId)
	{
		dictPermissionService.checkEditPermission(dictId, userId);

		var result = new ArrayList<DictDataItem>();

		try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8))
		{
//			TODO: валидация полей
			var format = CSVFormat.EXCEL.withDelimiter(',').withFirstRecordAsHeader().withSkipHeaderRecord();
			var parser = format.parse(reader);

			var headerMap = parser.getHeaderMap()
					.entrySet()
					.stream()
					.collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

			for (var record : parser)
			{
				var recordMap = new LinkedHashMap<String, Object>();

				for (int i = 0; i < record.size(); i++)
				{
					if (ServiceFieldConstants.getServiceInsertableFields().contains(headerMap.get(i)))
					{
						continue;
					}

					recordMap.put(headerMap.get(i), record.get(i));
				}

				result.add(DictDataItem.of(dictId, recordMap));
			}
		}
		catch (IOException e)
		{
			log.error("Ошибка при импорте справочника.", e);
		}

		return dictDataService.createMany(dictId, result, userId);
	}
}
