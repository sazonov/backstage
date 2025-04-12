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

package com.proit.app.dict.service;

import com.proit.app.database.model.Identity;
import com.proit.app.database.utils.StreamUtils;
import com.proit.app.dict.api.domain.DictFieldType;
import com.proit.app.dict.configuration.backend.provider.DictDataBackendProvider;
import com.proit.app.dict.constant.ServiceFieldConstants;
import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.domain.DictFieldName;
import com.proit.app.dict.domain.DictItem;
import com.proit.app.dict.model.dictitem.DictDataItem;
import com.proit.app.dict.service.advice.DictDataServiceAdvice;
import com.proit.app.dict.service.backend.DictDataBackend;
import com.proit.app.dict.service.lock.LockDictOperation;
import com.proit.app.dict.service.mapping.DictFieldNameMappingService;
import com.proit.app.dict.service.mapping.DictItemMappingService;
import com.proit.app.dict.service.query.QueryParser;
import com.proit.app.dict.service.validation.DictDataValidationService;
import com.proit.app.exception.AppException;
import com.proit.app.exception.ObjectNotFoundException;
import com.proit.app.model.other.exception.ApiStatusCodeImpl;
import com.proit.app.utils.SecurityUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	@LockDictOperation("#dictId")
	public List<DictItem> getByIds(String dictId, List<String> ids, List<String> selectFields, String userId)
	{
		var dict = dictService.getById(dictId);

		dictPermissionService.checkViewPermission(dict, userId);
		serviceAdviceList.forEach(it -> it.handleGetByIds(dict, ids));

		if (ids.isEmpty())
		{
			throw new AppException(ApiStatusCodeImpl.DICTS_ERROR, "Не указаны идентификаторы элементов справочника.");
		}

		var requiredFields = selectFields.stream()
				.map(dictFieldNameMappingService::mapDictFieldName)
				.toList();

		dictDataValidationService.validateSelectFields(dict, requiredFields);

		return backend(dict)
				.getByIds(dict, ids, requiredFields)
				.stream()
				.sorted(StreamUtils.listOrderComparator(ids))
				.toList();
	}

	public Page<DictItem> getByFilter(String dictId, List<String> selectFields, String filtersQuery, Pageable pageable)
	{
		return getByFilter(dictId, selectFields, filtersQuery, pageable, SecurityUtils.getCurrentUserId());
	}

	@LockDictOperation("#dictId")
	public Page<DictItem> getByFilter(String dictId, List<String> selectFields, String filtersQuery, Pageable pageable, String userId)
	{
		var dict = dictService.getById(dictId);

		dictPermissionService.checkViewPermission(dict, userId);

		dict.getFields()
				.stream()
				.filter(it -> it.getType() == DictFieldType.DICT)
				.forEach(it -> dictPermissionService.checkViewPermission(dictService.getById(it.getDictRef().getDictId()), userId));

		serviceAdviceList.forEach(it -> it.handleGetByFilter(dict, selectFields, filtersQuery, pageable));

		var requiredFields = selectFields.stream()
				.map(dictFieldNameMappingService::mapDictFieldName)
				.toList();

		dictDataValidationService.validateSelectFields(dict, requiredFields);
		dictDataValidationService.validatePageable(dict, pageable);

		return backend(dict).getByFilter(dict, requiredFields, queryParser.parse(filtersQuery), pageable);
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

	@LockDictOperation("#dictId")
	public boolean existsById(String dictId, String itemId, String userId)
	{
		var dict = dictService.getById(dictId);

		dictPermissionService.checkViewPermission(dict, userId);

		serviceAdviceList.forEach(it -> it.handleExistsById(dict, itemId));

		return backend(dict).existsById(dict, itemId);
	}

	public boolean existsByFilter(String dictId, String filtersQuery)
	{
		return existsByFilter(dictId, filtersQuery, SecurityUtils.getCurrentUserId());
	}

	@LockDictOperation("#dictId")
	public boolean existsByFilter(String dictId, String filtersQuery, String userId)
	{
		var dict = dictService.getById(dictId);

		dictPermissionService.checkViewPermission(dict, userId);

		serviceAdviceList.forEach(it -> it.handleExistsByFilter(dict, filtersQuery));

		return backend(dict).existsByFilter(dict, queryParser.parse(filtersQuery));
	}

	public long countByFilter(String dictId, String filtersQuery)
	{
		return countByFilter(dictId, filtersQuery, SecurityUtils.getCurrentUserId());
	}

	@LockDictOperation("#dictId")
	public long countByFilter(String dictId, String filtersQuery, String userId)
	{
		var dict = dictService.getById(dictId);

		dictPermissionService.checkViewPermission(dict, userId);

		serviceAdviceList.forEach(it -> it.handleCountByFilter(dict, filtersQuery));

		return backend(dict).countByFilter(dict, queryParser.parse(filtersQuery));
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

	@LockDictOperation("#dictDataItem.dictId")
	public DictItem create(DictDataItem dictDataItem, String userId)
	{
		var dictId = dictDataItem.getDictId();

		var dict = dictService.getById(dictId);

		dictPermissionService.checkEditPermission(dict, userId);
		serviceAdviceList.forEach(it -> it.handleBeforeCreate(dictDataItem));

		dictDataValidationService.validateDictDataItem(dictDataItem, userId);
		var dictItem = dictItemMappingService.mapDictItem(dictDataItem);

		setupServiceFields(dictItem);

		var result = backend(dict).create(dict, dictItem);

		serviceAdviceList.forEach(it -> it.handleAfterCreate(dict, result));

		return result;
	}

	@Deprecated(forRemoval = true)
	public List<DictItem> createMany(String dictId, List<Map<String, Object>> dictDataList)
	{
		return createMany(dictId, dictDataList.stream().map(it -> buildDictDataItem(dictId, it)).toList(), SecurityUtils.getCurrentUserId());
	}

	@LockDictOperation("#dictId")
	public List<DictItem> createMany(String dictId, List<DictDataItem> dictDataItems, String userId)
	{
		var dict = dictService.getById(dictId);

		dictPermissionService.checkEditPermission(dict, userId);
		serviceAdviceList.forEach(it -> it.handleBeforeCreateMany(dictDataItems));

		var dictItems = dictDataItems.stream()
				.peek(it -> dictDataValidationService.validateDictDataItem(it, userId))
				.map(dictItemMappingService::mapDictItem)
				.peek(this::setupServiceFields)
				.toList();

		var result = backend(dict).createMany(dict, dictItems);

		serviceAdviceList.forEach(it -> it.handleAfterCreateMany(dict, result));

		return result;
	}

	@Transactional
	@Deprecated(forRemoval = true)
	public DictItem update(String dictId, String itemId, long version, Map<String, Object> dictData)
	{
		return update(itemId, buildDictDataItem(dictId, dictData), version);
	}

	@Transactional
	@Deprecated(forRemoval = true)
	public DictItem update(String dictId, String itemId, long version, Map<String, Object> dictData, String userId)
	{
		return update(itemId, buildDictDataItem(dictId, dictData), version, userId);
	}

	@Transactional
	public DictItem update(String itemId, DictDataItem dictDataItem, long version)
	{
		return update(itemId, dictDataItem, version, SecurityUtils.getCurrentUserId());
	}

	@Transactional
	@LockDictOperation("#dictDataItem.dictId")
	public DictItem update(String itemId, DictDataItem dictDataItem, long version, String userId)
	{
		var dictId = dictDataItem.getDictId();

		var item = getById(dictId, itemId, userId);

		var dict = dictService.getById(dictId);

		dictPermissionService.checkEditPermission(dict, userId);
		serviceAdviceList.forEach(it -> it.handleUpdate(dict, item, dictDataItem));

		dictDataValidationService.validateDictDataItem(dictDataItem, userId);
		dictDataValidationService.validateOptimisticLock(dictId, itemId, version, userId);

		var mappedItem = dictItemMappingService.mapDictItem(dictDataItem);

		var dictItem = withUpdated(mappedItem, item);

		var result = backend(dict).update(dict, itemId, dictItem, version);

		serviceAdviceList.forEach(it -> it.handleAfterUpdate(dict, result));

		return result;
	}

	@Transactional
	public void delete(String dictId, String itemId, boolean deleted, long version)
	{
		delete(dictId, itemId, deleted, null, SecurityUtils.getCurrentUserId(), version);
	}

	@Transactional
	public void delete(String dictId, String itemId, boolean deleted, String reason, long version)
	{
		delete(dictId, itemId, deleted, reason, SecurityUtils.getCurrentUserId(), version);
	}

	@Transactional
	@LockDictOperation("#dictId")
	public void delete(String dictId, String itemId, boolean deleted, String reason, String userId, long version)
	{
		var item = getById(dictId, itemId, userId);

		var dict = dictService.getById(dictId);

		dictPermissionService.checkEditPermission(dict, userId);

		serviceAdviceList.forEach(it -> it.handleDelete(dict, item, deleted));

		dictDataValidationService.validateOptimisticLock(dictId, itemId, version, userId);

		var dictItem = withDeleted(deleted, reason, item);

		backend(dict).delete(dict, dictItem);
	}

	public void deleteAll(String dictId, boolean deleted)
	{
		deleteAll(dictId, deleted, SecurityUtils.getCurrentUserId());
	}

	@LockDictOperation("#dictId")
	public void deleteAll(String dictId, boolean deleted, String userId)
	{
		var dict = dictService.getById(dictId);

		dictPermissionService.checkEditPermission(dict, userId);
		serviceAdviceList.forEach(it -> it.handleDeleteAll(dict, deleted));

		var backend = backend(dictId);

		var dictItems = backend.getByFilter(dict, List.of(new DictFieldName(null, "*")), queryParser.parse("deleted = null"), Pageable.unpaged())
				.getContent()
				.stream()
				.peek(it -> dictDataValidationService.validateOptimisticLock(dictId, it.getId(), it.getVersion(), userId))
				.map(it -> withDeleted(deleted, null, it))
				.toList();

		backend.deleteAll(dict, dictItems);
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
				.filter(it -> !ServiceFieldConstants.ID.equals(it.getKey()))
				.collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap<String, Object>::putAll);

		historyItemMap.put(ServiceFieldConstants.VERSION, dictItem.getVersion());
		historyItemMap.put(ServiceFieldConstants.CREATED, dictItem.getCreated());
		historyItemMap.put(ServiceFieldConstants.UPDATED, dictItem.getUpdated());

		dictItem.setHistory(List.of(historyItemMap));
	}

	/**
	 * Id - устанавливается в target по кейсу: создание item с предопределенным id {@link ServiceFieldConstants#serviceInsertableFields}
	 */
	private DictItem withUpdated(DictItem source, DictItem target)
	{
		target.setId(source.getId() != null ? source.getId() : target.getId());
		target.setData(source.getData());
		target.setUpdated(LocalDateTime.now());
		target.setVersion(target.getVersion() + 1L);

		var historyMap = new HashMap<>(target.getData());

		historyMap.put(ServiceFieldConstants.UPDATED, target.getUpdated());
		historyMap.put(ServiceFieldConstants.VERSION, target.getVersion());

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

		historyMap.put(ServiceFieldConstants.UPDATED, dictItem.getUpdated());
		historyMap.put(ServiceFieldConstants.VERSION, dictItem.getVersion());
		historyMap.put(ServiceFieldConstants.DELETED, dictItem.getDeleted());
		historyMap.put(ServiceFieldConstants.DELETION_REASON, dictItem.getDeletionReason());

		dictItem.getHistory().add(historyMap);

		return dictItem;
	}
}
