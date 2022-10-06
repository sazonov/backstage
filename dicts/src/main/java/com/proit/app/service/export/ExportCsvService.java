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

import com.opencsv.CSVWriter;
import com.proit.app.constant.ServiceFieldConstants;
import com.proit.app.domain.DictField;
import com.proit.app.domain.DictItem;
import com.proit.app.exception.AppException;
import com.proit.app.model.api.ApiStatusCodeImpl;
import com.proit.app.service.DictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportCsvService implements ExportService
{
	private final DictService dictService;

	public byte[] export(String dictId, List<DictItem> items, String userId)
	{
		var systemFieldIds = List.of(ServiceFieldConstants.ID, ServiceFieldConstants.CREATED,
				ServiceFieldConstants.UPDATED, ServiceFieldConstants.DELETED, ServiceFieldConstants.VERSION);
		var dataFieldIds = dictService.getDataFieldsByDictId(dictId)
				.stream()
				.map(DictField::getId)
				.collect(Collectors.toList());

		var headers = Stream.concat(systemFieldIds.stream(), dataFieldIds.stream()).collect(Collectors.toList());

		var data = items.stream()
				.map(item -> mapDictItem(dataFieldIds, item))
				.collect(Collectors.toList());

		return writeToByteArray(headers, data);
	}

	private String[] mapDictItem(List<String> dataFieldIds, DictItem item)
	{
		var itemData = new ArrayList<>();

		itemData.add(item.getId());
		itemData.add(item.getCreated());
		itemData.add(item.getUpdated());
		itemData.add(item.getDeleted() != null);
		itemData.add(item.getVersion());
		itemData.addAll(dataFieldIds.stream()
				.map(it -> item.getData().getOrDefault(it, ""))
				.collect(Collectors.toList()));

		return itemData.stream()
				.map(String::valueOf)
				.toArray(String[]::new);
	}

	private byte[] writeToByteArray(List<String> headers, List<String[]> data)
	{
		try (var outputStream = new ByteArrayOutputStream();
		     var osw = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
		     var writer = new CSVWriter(osw))
		{
			writer.writeNext(headers.toArray(String[]::new), false);
			writer.writeAll(data, false);

			return outputStream.toByteArray();
		}
		catch (IOException e)
		{
			throw new AppException(ApiStatusCodeImpl.DICTS_ERROR, "При экспорте справочника произошла ошибка.", e);
		}
	}
}
