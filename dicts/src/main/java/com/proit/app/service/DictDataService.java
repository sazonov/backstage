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

import com.proit.app.configuration.backend.provider.DictDataBackendProvider;
import com.proit.app.domain.Dict;
import com.proit.app.domain.DictFieldName;
import com.proit.app.domain.DictFieldType;
import com.proit.app.domain.DictItem;
import com.proit.app.exception.AppException;
import com.proit.app.exception.ObjectNotFoundException;
import com.proit.app.model.api.ApiStatusCodeImpl;
import com.proit.app.model.dictitem.DictDataItem;
import com.proit.app.model.domain.Identity;
import com.proit.app.service.advice.DictDataServiceAdvice;
import com.proit.app.service.backend.DictDataBackend;
import com.proit.app.service.mapping.DictFieldNameMappingService;
import com.proit.app.service.mapping.DictItemMappingService;
import com.proit.app.service.query.QueryParser;
import com.proit.app.service.validation.DictDataValidationService;
import com.proit.app.utils.SecurityUtils;
import com.proit.app.utils.StreamUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.proit.app.constant.ServiceFieldConstants.*;

@Service
@RequiredArgsConstructor
public class DictDataService
{
	private final DictService dictService;
	private final DictPermissionService dictPermissionService;
	private final DictDataValidationService dictDataValidationService;

	private final DictItemMappingService dictItemMappingService;
	private final DictFieldNameMappingService dictFieldNameMappingService;

	private final QueryParser queryParser;

	private final DictDataBackendProvider backendProvider;

	@Getter
	private final List<DictDataServiceAdvice> serviceAdviceList;

	//TODO: Провести рефакторинг
	// создать апи для getDictById с проверкой секьюрити и валидацией.
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
				.map(dictFieldNameMappingService::mapDictFieldName)
				.toList();

		dictDataValidationService.validateSelectFields(dictId, requiredFields);

		return backend(dictId)
				.getByIds(dictId, ids, requiredFields)
				.stream()
				.sorted(StreamUtils.listOrderComparator(ids))
				.toList();
	}

	public Page<DictItem> getByFilter(String dictId, List<String> selectFields, String filtersQuery, Pageable pageable)
	{
		return getByFilter(dictId, selectFields, filtersQuery, pageable, SecurityUtils.getCurrentUserId());
	}

	//TODO: реализовать валидацию filtersQuery и маппинг в QueryExpression с учетом сопоставления констант с типом DictField
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
				.map(dictFieldNameMappingService::mapDictFieldName)
				.toList();

		dictDataValidationService.validateSelectFields(dictId, requiredFields);
		dictDataValidationService.validatePageable(dictId, pageable);

		return backend(dict).getByFilter(dictId, requiredFields, queryParser.parse(filtersQuery), pageable);
	}

	public Page<String> getIdsByFilter(String dictId, String filtersQuery)
	{
		return getIdsByFilter(dictId, filtersQuery, SecurityUtils.getCurrentUserId());
	}

	public Page<String> getIdsByFilter(String dictId, String filtersQuery, String userId)
	{
		return getByFilter(dictId, List.of("id"), filtersQuery, Pageable.unpaged(), userId)
				.map(Identity::getId);
	}

	public boolean existsById(String dictId, String itemId)
	{
		return existsById(dictId, itemId, SecurityUtils.getCurrentUserId());
	}

	public boolean existsById(String dictId, String itemId, String userId)
	{
		dictPermissionService.checkViewPermission(dictId, userId);

		serviceAdviceList.forEach(it -> it.handleExistsById(dictId, itemId));

		return backend(dictId).existsById(dictId, itemId);
	}

	public boolean existsByFilter(String dictId, String filtersQuery)
	{
		return existsByFilter(dictId, filtersQuery, SecurityUtils.getCurrentUserId());
	}

	public boolean existsByFilter(String dictId, String filtersQuery, String userId)
	{
		dictPermissionService.checkViewPermission(dictId, userId);

		serviceAdviceList.forEach(it -> it.handleExistsByFilter(dictId, filtersQuery));

		return backend(dictId).existsByFilter(dictId, queryParser.parse(filtersQuery));
	}

	public long countByFilter(String dictId, String filtersQuery)
	{
		return countByFilter(dictId, filtersQuery, SecurityUtils.getCurrentUserId());
	}

	public long countByFilter(String dictId, String filtersQuery, String userId)
	{
		dictPermissionService.checkViewPermission(dictId, userId);

		serviceAdviceList.forEach(it -> it.handleCountByFilter(dictId, filtersQuery));

		return backend(dictId).countByFilter(dictId, queryParser.parse(filtersQuery));
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

		dictDataValidationService.validateDictDataItem(dictDataItem, userId);
		var dictItem = dictItemMappingService.mapDictItem(dictDataItem);

		setupServiceFields(dictItem);

		var result = backend(dictId).create(dictId, dictItem);

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
				.peek(it -> dictDataValidationService.validateDictDataItem(it, userId))
				.map(dictItemMappingService::mapDictItem)
				.peek(this::setupServiceFields)
				.toList();

		var result = backend(dictId).createMany(dictId, dictItems);

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

		var item = getById(dictId, itemId, userId);

		dictPermissionService.checkEditPermission(dictId, userId);
		serviceAdviceList.forEach(it -> it.handleUpdate(item, dictDataItem));

		dictDataValidationService.validateDictDataItem(dictDataItem, userId);
		dictDataValidationService.validateOptimisticLock(dictId, itemId, version, userId);

		var mappedItem = dictItemMappingService.mapDictItem(dictDataItem);

		var dictItem = withUpdated(mappedItem, item);

		var result = backend(dictId).update(dictId, itemId, dictItem, version);

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
		var item = getById(dictId, itemId, userId);

		dictPermissionService.checkEditPermission(dictId, userId);

		serviceAdviceList.forEach(it -> it.handleDelete(dictId, item, deleted));

		dictDataValidationService.validateOptimisticLock(dictId, itemId, item, userId);

		var dictItem = withDeleted(deleted, reason, item);

		backend(dictId).delete(dictId, dictItem);
	}

	public void deleteAll(String dictId, boolean deleted)
	{
		deleteAll(dictId, deleted, SecurityUtils.getCurrentUserId());
	}

	public void deleteAll(String dictId, boolean deleted, String userId)
	{
		dictPermissionService.checkEditPermission(dictId, userId);
		serviceAdviceList.forEach(it -> it.handleDeleteAll(dictId, deleted));

		var backend = backend(dictId);

		var dictItems = backend.getByFilter(dictId, List.of(new DictFieldName(null, "*")), queryParser.parse("deleted = null"), Pageable.unpaged())
				.getContent()
				.stream()
				.peek(it -> dictDataValidationService.validateOptimisticLock(dictId, it.getId(), it, userId))
				.map(it -> withDeleted(deleted, null, it))
				.toList();

		backend.deleteAll(dictId, dictItems);
	}

	private DictDataBackend backend(String dictId)
	{
		return backend(dictService.getById(dictId));
	}

	private DictDataBackend backend(Dict dict)
	{
		return backendProvider.getBackendByEngineName(dict.getEngine().getName());
	}

	private DictDataItem buildDictDataItem(String dictId, Map<String, Object> dictData)
	{
		return DictDataItem.of(dictId, dictData);
	}

	private void setupServiceFields(DictItem dictItem)
	{
		dictItem.setVersion(1L);
		dictItem.setCreated(LocalDateTime.now());
		dictItem.setUpdated(LocalDateTime.now());
		dictItem.setDeleted(null);
		dictItem.setDeletionReason(null);

		setupHistoryItemMap(dictItem);
	}

	private void setupHistoryItemMap(DictItem dictItem)
	{
		var historyItemMap = dictItem.getData()
				.entrySet()
				.stream()
				.filter(it -> !ID.equals(it.getKey()))
				.collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap<String, Object>::putAll);

		historyItemMap.put(VERSION, dictItem.getVersion());
		historyItemMap.put(CREATED, dictItem.getCreated());
		historyItemMap.put(UPDATED, dictItem.getUpdated());

		dictItem.setHistory(List.of(historyItemMap));
	}

	/**
	 * Id - устанавливается в target по кейсу: создание item с предопределенным id {@link com.proit.app.constant.ServiceFieldConstants#serviceInsertableFields}
	 */
	private DictItem withUpdated(DictItem source, DictItem target)
	{
		target.setId(source.getId() != null ? source.getId() : target.getId());
		target.setData(source.getData());
		target.setUpdated(LocalDateTime.now());
		target.setVersion(target.getVersion() + 1L);

		var historyMap = new HashMap<>(target.getData());

		historyMap.put(UPDATED, target.getUpdated());
		historyMap.put(VERSION, target.getVersion());

		target.getHistory().add(historyMap);

		return target;
	}

	private DictItem withDeleted(boolean deleted, String reason, DictItem dictItem)
	{
		dictItem.setUpdated(LocalDateTime.now());
		dictItem.setVersion(dictItem.getVersion() + 1L);

		if (deleted)
		{
			dictItem.setDeleted(LocalDateTime.now());
			dictItem.setDeletionReason(StringUtils.isBlank(reason) ? null : reason);
		}
		else
		{
			dictItem.setDeleted(null);
			dictItem.setDeletionReason(null);
		}

		var historyMap = new HashMap<String, Object>();

		historyMap.put(UPDATED, dictItem.getUpdated());
		historyMap.put(VERSION, dictItem.getVersion());
		historyMap.put(DELETED, dictItem.getDeleted());
		historyMap.put(DELETION_REASON, dictItem.getDeletionReason());

		dictItem.getHistory().add(historyMap);

		return dictItem;
	}
}
