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

package com.proit.app.service;

import com.proit.app.constant.ServiceFieldConstants;
import com.proit.app.domain.DictFieldType;
import com.proit.app.domain.DictItem;
import com.proit.app.exception.AppException;
import com.proit.app.model.api.ApiStatusCodeImpl;
import com.proit.app.service.advice.DictDataServiceAdvice;
import com.proit.app.service.backend.MongoDictBackend;
import com.proit.app.utils.SecurityUtils;
import com.proit.app.utils.StreamUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DictDataService
{
	private final DictService dictService;
	private final MappingService mappingService;
	private final ValidationService validationService;
	private final DictPermissionService dictPermissionService;

	private final MongoDictBackend mongoDictBackend;

	@Getter
	private final List<DictDataServiceAdvice> serviceAdviceList;

	public DictItem getById(String dictId, String itemId)
	{
		var dictFieldNames = List.of(mappingService.mapDictFieldName("*"));

		validationService.validateSelectFields(dictId, dictFieldNames);

		return mappingService.mapDictItem(dictId, mongoDictBackend.getById(dictId, itemId, dictFieldNames));
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
				.map(mappingService::mapDictFieldName)
				.toList();

		validationService.validateSelectFields(dictId, requiredFields);

		return mongoDictBackend.getByIds(dictId, ids, requiredFields)
				.stream()
				.map(it -> mappingService.mapDictItem(dictId, it))
				.sorted(StreamUtils.listOrderComparator(ids))
				.toList();
	}

	public Page<DictItem> getByFilter(String dictId, List<String> selectFields, String filtersQuery, Pageable pageable)
	{
		return getByFilter(dictId, selectFields, filtersQuery, pageable, SecurityUtils.getCurrentUserId());
	}

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
				.map(mappingService::mapDictFieldName)
				.toList();

		validationService.validateSelectFields(dictId, requiredFields);

		var customizedPageable = pageable.getSort().isEmpty()
				? pageable
				: PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().and(Sort.by(Sort.Direction.ASC, ServiceFieldConstants._ID)));

		return mongoDictBackend.getByFilter(dict, requiredFields, filtersQuery, customizedPageable)
				.map(it -> mappingService.mapDictItem(dictId, it));
	}

	public boolean existsById(String dictId, String itemId)
	{
		return existsById(dictId, itemId, SecurityUtils.getCurrentUserId());
	}

	public boolean existsById(String dictId, String itemId, String userId)
	{
		dictPermissionService.checkViewPermission(dictId, userId);

		serviceAdviceList.forEach(it -> it.handleExistsById(dictId, itemId));

		return mongoDictBackend.existsById(dictId, itemId);
	}

	public boolean existsByFilter(String dictId, String filtersQuery)
	{
		return existsByFilter(dictId, filtersQuery, SecurityUtils.getCurrentUserId());
	}

	public boolean existsByFilter(String dictId, String filtersQuery, String userId)
	{
		dictPermissionService.checkViewPermission(dictId, userId);

		serviceAdviceList.forEach(it -> it.handleExistsByFilter(dictId, filtersQuery));

		return mongoDictBackend.existsByFilter(dictId, filtersQuery);
	}

	public long countByFilter(String dictId, String filtersQuery)
	{
		return countByFilter(dictId, filtersQuery, SecurityUtils.getCurrentUserId());
	}

	public long countByFilter(String dictId, String filtersQuery, String userId)
	{
		dictPermissionService.checkViewPermission(dictId, userId);

		serviceAdviceList.forEach(it -> it.handleCountByFilter(dictId, filtersQuery));

		return mongoDictBackend.countByFilter(dictId, filtersQuery);
	}

	public DictItem create(String dictId, Map<String, Object> doc)
	{
		return create(dictId, doc, SecurityUtils.getCurrentUserId());
	}

	public DictItem create(String dictId, Map<String, Object> doc, String userId)
	{
		dictPermissionService.checkEditPermission(dictId, userId);
		serviceAdviceList.forEach(it -> it.handleBeforeCreate(dictId, doc));

		validationService.validateDocInsert(dictId, doc);
		var mappedDoc = mappingService.mapDictDoc(dictId, doc);

		var result = mappingService.mapDictItem(dictId, mongoDictBackend.create(dictId, mappedDoc));

		serviceAdviceList.forEach(it -> it.handleAfterCreate(dictId, result));

		return result;
	}

	public List<DictItem> createMany(String dictId, List<Map<String, Object>> docs)
	{
		return createMany(dictId, docs, SecurityUtils.getCurrentUserId());
	}

	public List<DictItem> createMany(String dictId, List<Map<String, Object>> docs, String userId)
	{
		dictPermissionService.checkEditPermission(dictId, userId);
		serviceAdviceList.forEach(it -> it.handleBeforeCreateMany(dictId, docs));

		var mappedDocs = docs.stream()
				.peek(it -> validationService.validateDocInsert(dictId, it))
				.map(it -> mappingService.mapDictDoc(dictId, it))
				.toList();

		var result = mongoDictBackend.createMany(dictId, mappedDocs)
				.stream()
				.map(it -> mappingService.mapDictItem(dictId, it))
				.toList();

		serviceAdviceList.forEach(it -> it.handleAfterCreateMany(dictId, result));

		return result;
	}

	public DictItem update(String dictId, String itemId, long version, Map<String, Object> doc)
	{
		return update(dictId, itemId, version, doc, SecurityUtils.getCurrentUserId());
	}

	public DictItem update(String dictId, String itemId, long version, Map<String, Object> doc, String userId)
	{
		var dictItem = getById(dictId, itemId);

		dictPermissionService.checkEditPermission(dictId, userId);
		serviceAdviceList.forEach(it -> it.handleUpdate(dictId, dictItem, doc));

		validationService.validateDocInsert(dictId, doc);

		var mappedDoc = mappingService.mapDictDoc(dictId, doc);

		return mappingService.mapDictItem(dictId, mongoDictBackend.update(dictId, itemId, version, mappedDoc));
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
		var dictItem = getById(dictId, itemId);

		dictPermissionService.checkEditPermission(dictId, userId);

		serviceAdviceList.forEach(it -> it.handleDelete(dictId, dictItem, deleted));

		mongoDictBackend.delete(dictId, itemId, deleted, reason);
	}

	public void deleteAll(String dictId, boolean deleted)
	{
		deleteAll(dictId, deleted, SecurityUtils.getCurrentUserId());
	}

	public void deleteAll(String dictId, boolean deleted, String userId)
	{
		dictPermissionService.checkEditPermission(dictId, userId);
		serviceAdviceList.forEach(it -> it.handleDeleteAll(dictId, deleted));

		mongoDictBackend.deleteAll(dictId, deleted);
	}
}
