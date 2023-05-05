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

package com.proit.app.service;

import com.proit.app.domain.DictFieldType;
import com.proit.app.domain.DictItem;
import com.proit.app.exception.AppException;
import com.proit.app.exception.ObjectNotFoundException;
import com.proit.app.model.api.ApiStatusCodeImpl;
import com.proit.app.model.dictitem.DictDataItem;
import com.proit.app.model.dictitem.FieldDataItem;
import com.proit.app.service.advice.DictDataServiceAdvice;
import com.proit.app.service.backend.DictDataBackend;
import com.proit.app.service.mapping.DictDataItemDictItemMapper;
import com.proit.app.service.mapping.FieldDataItemDictFieldNameMapper;
import com.proit.app.service.query.QueryParser;
import com.proit.app.service.validation.DictDataValidationService;
import com.proit.app.utils.SecurityUtils;
import com.proit.app.utils.StreamUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.proit.app.constant.ServiceFieldConstants.*;

@Service
@RequiredArgsConstructor
public class DictDataService
{
	private final DictService dictService;
	private final DictPermissionService dictPermissionService;
	private final DictDataValidationService dictDataValidationService;

	private final DictDataItemDictItemMapper dictDataItemDictItemMapper;
	private final FieldDataItemDictFieldNameMapper fieldDataItemDictFieldNameMapper;

	private final DictDataBackend dictDataBackend;

	private final QueryParser queryParser;

	@Getter
	private final List<DictDataServiceAdvice> serviceAdviceList;

	//TODO: Провести рефакторинг
	// создать апи для getById с проверкой секьюрити и валидацией.
	public DictItem getById(String dictId, String itemId)
	{
		return getById(dictId, itemId, SecurityUtils.getCurrentUserId());
	}

	public DictItem getById(String dictId, String itemId, String userId)
	{
		return getByIds(dictId, List.of(itemId), userId)
				.stream()
				.findFirst()
				.orElseThrow(() -> new ObjectNotFoundException(DictItem.class, "dictId: %s, itemId: %s".formatted(dictId, itemId)));
	}

	public List<DictItem> getByIds(String dictId, List<String> ids)
	{
		return getByIds(dictId, ids, SecurityUtils.getCurrentUserId());
	}

	public List<DictItem> getByIds(String dictId, List<String> ids, String userId)
	{
		return getByIds(dictId, ids, List.of("*"), userId);
	}

	public List<DictItem> getByIds(String dictId, List<String> ids, List<String> selectFields, String userId)
	{
		dictPermissionService.checkViewPermission(dictId, userId);
		serviceAdviceList.forEach(it -> it.handleGetByIds(dictId, ids));

		if (ids.isEmpty())
		{
			throw new AppException(ApiStatusCodeImpl.DICTS_ERROR, "Не указаны идентификаторы элементов справочника.");
		}

		var requiredFields = selectFields.stream()
				.map(this::buildFieldDataItem)
				.map(fieldDataItemDictFieldNameMapper::map)
				.toList();

		dictDataValidationService.validateSelectFields(dictId, requiredFields);

		return dictDataBackend.getByIds(dictId, ids, requiredFields)
				.stream()
				.sorted(StreamUtils.listOrderComparator(ids))
				.toList();
	}

	public Page<DictItem> getByFilter(String dictId, List<String> selectFields, String filtersQuery, Pageable pageable)
	{
		return getByFilter(dictId, selectFields, filtersQuery, pageable, SecurityUtils.getCurrentUserId());
	}

	//TODO: При реализации дополнительного адаптера, осуществить валидацию pageable (в т.н. сортировки), selectFields, filtersQuery на наличие переданных клиентом
	// полей характерных для монго адаптера (_id). Рассмотреть возможность реализации валидации в эдвайсах.
	public Page<DictItem> getByFilter(String dictId, List<String> selectFields, String filtersQuery, Pageable pageable, String userId)
	{
		dictPermissionService.checkViewPermission(dictId, userId);

		var dict = dictService.getById(dictId);

		dict.getFields()
				.stream()
				.filter(it -> it.getType() == DictFieldType.DICT)
				.forEach(it -> dictPermissionService.checkViewPermission(it.getDictRef().getDictId(), userId));

		serviceAdviceList.forEach(it -> it.handleGetByFilter(dictId, selectFields, filtersQuery, pageable));

		var requiredFields = selectFields.stream()
				.map(this::buildFieldDataItem)
				.map(fieldDataItemDictFieldNameMapper::map)
				.toList();

		dictDataValidationService.validateSelectFields(dictId, requiredFields);

		return dictDataBackend.getByFilter(dictId, requiredFields, queryParser.parse(filtersQuery), pageable);
	}

	public boolean existsById(String dictId, String itemId)
	{
		return existsById(dictId, itemId, SecurityUtils.getCurrentUserId());
	}

	public boolean existsById(String dictId, String itemId, String userId)
	{
		dictPermissionService.checkViewPermission(dictId, userId);

		serviceAdviceList.forEach(it -> it.handleExistsById(dictId, itemId));

		return dictDataBackend.existsById(dictId, itemId);
	}

	public boolean existsByFilter(String dictId, String filtersQuery)
	{
		return existsByFilter(dictId, filtersQuery, SecurityUtils.getCurrentUserId());
	}

	public boolean existsByFilter(String dictId, String filtersQuery, String userId)
	{
		dictPermissionService.checkViewPermission(dictId, userId);

		serviceAdviceList.forEach(it -> it.handleExistsByFilter(dictId, filtersQuery));

		return dictDataBackend.existsByFilter(dictId, queryParser.parse(filtersQuery));
	}

	public long countByFilter(String dictId, String filtersQuery)
	{
		return countByFilter(dictId, filtersQuery, SecurityUtils.getCurrentUserId());
	}

	public long countByFilter(String dictId, String filtersQuery, String userId)
	{
		dictPermissionService.checkViewPermission(dictId, userId);

		serviceAdviceList.forEach(it -> it.handleCountByFilter(dictId, filtersQuery));

		return dictDataBackend.countByFilter(dictId, queryParser.parse(filtersQuery));
	}

	@Deprecated(forRemoval = true)
	public DictItem create(String dictId, Map<String, Object> dictData)
	{
		return create(buildDictDataItem(dictId, dictData));
	}

	@Deprecated(forRemoval = true)
	public DictItem create(String dictId, Map<String, Object> dictData, String userId)
	{
		return create(buildDictDataItem(dictId, dictData), userId);
	}

	public DictItem create(DictDataItem dictDataItem)
	{
		return create(dictDataItem, SecurityUtils.getCurrentUserId());
	}

	public DictItem create(DictDataItem dictDataItem, String userId)
	{
		var dictId = dictDataItem.getDictId();

		dictPermissionService.checkEditPermission(dictId, userId);
		serviceAdviceList.forEach(it -> it.handleBeforeCreate(dictDataItem));

		dictDataValidationService.validateDictDataItem(dictDataItem);
		var dictItem = dictDataItemDictItemMapper.map(dictDataItem);

		setServiceDictItemFields(dictItem);

		var result = dictDataBackend.create(dictId, dictItem);

		serviceAdviceList.forEach(it -> it.handleAfterCreate(dictId, result));

		return result;
	}

	//TODO: перевести вызовы в тестах на вызовы с DictDataItem
	@Deprecated(forRemoval = true)
	public List<DictItem> createMany(String dictId, List<Map<String, Object>> dictDataList)
	{
		return createMany(dictId, dictDataList.stream().map(it -> buildDictDataItem(dictId, it)).toList(), SecurityUtils.getCurrentUserId());
	}

	public List<DictItem> createMany(String dictId, List<DictDataItem> dictDataItems, String userId)
	{
		dictPermissionService.checkEditPermission(dictId, userId);
		serviceAdviceList.forEach(it -> it.handleBeforeCreateMany(dictDataItems));

		var dictItems = dictDataItems.stream()
				.peek(dictDataValidationService::validateDictDataItem)
				.map(dictDataItemDictItemMapper::map)
				.peek(this::setServiceDictItemFields)
				.toList();

		var result = dictDataBackend.createMany(dictId, dictItems);

		serviceAdviceList.forEach(it -> it.handleAfterCreateMany(dictId, result));

		return result;
	}

	@Deprecated(forRemoval = true)
	public DictItem update(String dictId, String itemId, long version, Map<String, Object> dictData)
	{
		return update(itemId, buildDictDataItem(dictId, dictData), version);
	}

	@Deprecated(forRemoval = true)
	public DictItem update(String dictId, String itemId, long version, Map<String, Object> dictData, String userId)
	{
		return update(itemId, buildDictDataItem(dictId, dictData), version, userId);
	}

	public DictItem update(String itemId, DictDataItem dictDataItem, long version)
	{
		return update(itemId, dictDataItem, version, SecurityUtils.getCurrentUserId());
	}

	public DictItem update(String itemId, DictDataItem dictDataItem, long version, String userId)
	{
		var dictId = dictDataItem.getDictId();

		var dictItem = getById(dictId, itemId, userId);

		dictPermissionService.checkEditPermission(dictId, userId);
		serviceAdviceList.forEach(it -> it.handleUpdate(dictItem, dictDataItem));

		dictDataValidationService.validateDictDataItem(dictDataItem);
		var mappedDictItem = dictDataItemDictItemMapper.map(dictDataItem);

		var result = dictDataBackend.update(dictId, itemId, version, mappedDictItem);

		serviceAdviceList.forEach(it -> it.handleAfterUpdate(dictId, result));

		return result;
	}

	public void delete(String dictId, String itemId, boolean deleted)
	{
		delete(dictId, itemId, deleted, null, SecurityUtils.getCurrentUserId());
	}

	public void delete(String dictId, String itemId, boolean deleted, String reason)
	{
		delete(dictId, itemId, deleted, reason, SecurityUtils.getCurrentUserId());
	}

	public void delete(String dictId, String itemId, boolean deleted, String reason, String userId)
	{
		var dictItem = getById(dictId, itemId, userId);

		dictPermissionService.checkEditPermission(dictId, userId);

		serviceAdviceList.forEach(it -> it.handleDelete(dictId, dictItem, deleted));

		dictDataBackend.delete(dictId, itemId, deleted, reason);
	}

	public void deleteAll(String dictId, boolean deleted)
	{
		deleteAll(dictId, deleted, SecurityUtils.getCurrentUserId());
	}

	public void deleteAll(String dictId, boolean deleted, String userId)
	{
		dictPermissionService.checkEditPermission(dictId, userId);
		serviceAdviceList.forEach(it -> it.handleDeleteAll(dictId, deleted));

		dictDataBackend.deleteAll(dictId, deleted);
	}

	private DictDataItem buildDictDataItem(String dictId, Map<String, Object> dictData)
	{
		return DictDataItem.of(dictId, dictData);
	}

	private void setServiceDictItemFields(DictItem dictItem)
	{
		var dataItemMap = dictItem.getData();

		dataItemMap.put(CREATED, new Date());
		dataItemMap.put(UPDATED, new Date());
		dataItemMap.put(DELETED, null);
		dataItemMap.put(VERSION, 1L);

		setHistoryItemMap(dictItem);
	}

	private void setHistoryItemMap(DictItem dictItem)
	{
		var historyItemMap = dictItem.getData()
				.entrySet()
				.stream()
				.filter(entry -> !entry.getKey().equalsIgnoreCase(ID))
				.collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);

		dictItem.getData().put(HISTORY, List.of(historyItemMap));
	}

	private FieldDataItem buildFieldDataItem(String selectField)
	{
		Supplier<String[]> dictField = () -> selectField.split("\\.");

		return selectField.contains(".")
				? FieldDataItem.builder().dictId(dictField.get()[0]).fieldItem(dictField.get()[1]).build()
				: FieldDataItem.builder().fieldItem(selectField).build();
	}
}
