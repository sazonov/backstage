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

package com.proit.app.dict.service.imp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.proit.app.dict.constant.ServiceFieldConstants;
import com.proit.app.dict.domain.DictField;
import com.proit.app.dict.domain.DictFieldName;
import com.proit.app.dict.domain.DictItem;
import com.proit.app.dict.model.dictitem.DictDataItem;
import com.proit.app.dict.service.DictDataService;
import com.proit.app.dict.service.DictPermissionService;
import com.proit.app.dict.service.DictService;
import com.proit.app.dict.api.domain.DictFieldType;
import com.proit.app.utils.SecurityUtils;
import com.proit.app.utils.StreamCollectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

//TODO: перевести импорт на вызов методов DictDataService
// с параметром DictDataItem вместо мапы
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportJsonService implements ImportService
{
	private final JsonReader jsonReader;

	private final DictService dictService;
	private final DictDataService dictDataService;
	private final DictPermissionService dictPermissionService;

	public List<DictItem> importDict(String dictId, InputStream inputStream)
	{
		return importDict(dictId, inputStream, SecurityUtils.getCurrentUserId());
	}

	public List<DictItem> importDict(String dictId, InputStream inputStream, String userId)
	{
		var dict = dictService.getById(dictId);

		dictPermissionService.checkEditPermission(dict, userId);

		Set<String> innerDictIds = getInnerDictIds(dictId);

		Map<String, List<Map<String, Object>>> dicts = jsonReader.readFromStream(inputStream, new TypeReference<>() { });

		return Optional.ofNullable(dicts.get(dictId))
				.map(parsedDict -> saveDict(dictId, innerDictIds, parsedDict, userId))
				.orElse(List.of());
	}

	private Set<String> getInnerDictIds(String dictId)
	{
		return dictService.getById(dictId).getFields()
				.stream()
				.filter(dictField -> dictField.getType() == DictFieldType.DICT)
				.map(DictField::getDictRef)
				.map(DictFieldName::getDictId)
				.collect(Collectors.toSet());
	}

	private List<DictItem> saveDict(final String dictId, Set<String> innerDictIds, List<Map<String, Object>> dict, String userId)
	{
		var innerDicts = readInnerDicts(innerDictIds, dict);

		saveInnerDicts(innerDicts, userId);

		return dict.stream()
				.peek(dictData -> innerDictIds.forEach(dictData::remove))
				.map(dictData -> dictDataService.create(DictDataItem.of(dictId, mapDoc(dictData)), userId))
				.toList();
	}

	private void saveInnerDicts(Map<String, List<Map<String, Object>>> innerDicts, String userId)
	{
		innerDicts.forEach((dictId, dictDataList) -> {
			var dictDataItems = dictDataList.stream()
					.map(it -> DictDataItem.of(dictId, it))
					.toList();

			dictDataService.createMany(dictId, dictDataItems, userId);
		});
	}

	private Map<String, List<Map<String, Object>>> readInnerDicts(Set<String> innerDictIds, List<Map<String, Object>> dict)
	{
		return dict.stream()
				.map(doc -> mapInnerDictDoc(innerDictIds, doc))
				.map(Map::entrySet)
				.flatMap(Collection::stream)
				.distinct()
				.collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
	}

	private Map<String, Map<String, Object>> mapInnerDictDoc(Set<String> innerDictIds, Map<String, Object> doc)
	{
		return innerDictIds.stream()
				.filter(id -> doc.get(id) instanceof Map<?, ?> map && map.containsKey(ServiceFieldConstants.ID))
				.map(id -> Map.entry(id, mapDoc((Map<String, Object>) doc.get(id))))
				.collect(StreamCollectors.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private Map<String, Object> mapDoc(Map<String, Object> item)
	{
		return item.entrySet()
				.stream()
				.map(this::mapDocItem)
				.filter(entry -> !ServiceFieldConstants.getServiceInsertableFields().contains(entry.getKey()))
				.collect(StreamCollectors.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private Map.Entry<String, Object> mapDocItem(Map.Entry<String, Object> entry)
	{
		return entry.getKey().equals(ServiceFieldConstants.ID)
				? Map.entry(ServiceFieldConstants._ID, entry.getValue())
				: entry;
	}
}
