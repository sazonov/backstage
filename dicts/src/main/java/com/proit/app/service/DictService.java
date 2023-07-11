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

import com.proit.app.constant.ServiceFieldConstants;
import com.proit.app.domain.*;
import com.proit.app.exception.dictionary.constraint.ConstraintAlreadyExistsException;
import com.proit.app.exception.dictionary.constraint.ConstraintNotFoundException;
import com.proit.app.exception.dictionary.enums.EnumAlreadyExistsException;
import com.proit.app.exception.dictionary.enums.EnumNotFoundException;
import com.proit.app.exception.dictionary.field.FieldNotFoundException;
import com.proit.app.exception.dictionary.index.IndexAlreadyExistsException;
import com.proit.app.exception.dictionary.index.IndexNotFoundException;
import com.proit.app.service.backend.DictBackend;
import com.proit.app.service.validation.DictValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.proit.app.constant.ServiceFieldConstants.*;

@Service
@RequiredArgsConstructor
public class DictService
{
	private final DictBackend dictBackend;

	private final DictValidationService dictValidationService;

	public Dict getById(String id)
	{
		return dictBackend.getDictById(id);
	}

	public List<DictField> getDataFieldsByDictId(String id)
	{
		return getDataFieldsByDict(getById(id));
	}

	public List<DictField> getDataFieldsByDict(Dict dict)
	{
		return dict.getFields()
				.stream()
				.filter(it -> !ServiceFieldConstants.getServiceSchemeFields().contains(it.getId()))
				.collect(Collectors.toList());
	}

	public List<Dict> getAll()
	{
		return dictBackend.getAllDicts();
	}

	public Dict create(Dict dict)
	{
		dictValidationService.validateDictScheme(dict);

		return dictBackend.createDict(buildScheme(dict, new Dict()));
	}

	//	TODO: История изменений схемы, даты создания/обновления схемы?
	public void delete(String id, boolean deleted)
	{
		dictBackend.deleteDict(id, deleted ? LocalDateTime.now() : null);
	}

	public Dict update(String dictId, Dict dict)
	{
		dictValidationService.validateDictScheme(dict);

		return dictBackend.updateDict(dictId, buildScheme(dict, getById(dictId)));
	}

	public DictField renameField(String dictId, String fieldId, String newFieldId, String newFieldName)
	{
		var dict = getById(dictId);

		var field = dict.getFields()
				.stream()
				.filter(it -> it.getId().equals(fieldId))
				.peek(it -> {
					it.setId(newFieldId);
					it.setName(newFieldName == null ? it.getName() : newFieldName);
				})
				.findFirst()
				.orElseThrow(() -> new FieldNotFoundException(dictId, fieldId));

		return dictBackend.renameDictField(dict, fieldId, field);
	}

	public DictConstraint createConstraint(String dictId, DictConstraint constraint)
	{
		var dict = getById(dictId);

//		TODO: Валидация - в validationService
		if (dict.getConstraints().stream().anyMatch(it -> it.getId().equals(constraint.getId())))
		{
			throw new ConstraintAlreadyExistsException(dictId, constraint.getId());
		}

		if (dict.getIndexes().stream().anyMatch(it -> it.getId().equals(constraint.getId())))
		{
			throw new IndexAlreadyExistsException(dictId, constraint.getId());
		}

		return dictBackend.createConstraint(dict, constraint);
	}

	public void deleteConstraint(String dictId, String id)
	{
		var dict = getById(dictId);

		if (dict.getConstraints().stream().noneMatch(it -> it.getId().equals(id)))
		{
			throw new ConstraintNotFoundException(dictId, id);
		}

		dictBackend.deleteConstraint(dict, id);
	}

	public DictIndex createIndex(String dictId, DictIndex index)
	{
		var dict = getById(dictId);

//		TODO: Валидация - в validationService
		if (dict.getIndexes().stream().anyMatch(it -> it.getId().equals(index.getId())))
		{
			throw new IndexAlreadyExistsException(dictId, index.getId());
		}

		if (dict.getConstraints().stream().anyMatch(it -> it.getId().equals(index.getId())))
		{
			throw new ConstraintAlreadyExistsException(dictId, index.getId());
		}

		return dictBackend.createIndex(dict, index);
	}

	public void deleteIndex(String dictId, String id)
	{
		var dict = getById(dictId);

		if (dict.getIndexes().stream().noneMatch(it -> it.getId().equals(id)))
		{
			throw new IndexNotFoundException(dictId, id);
		}

		dictBackend.deleteIndex(dict, id);
	}

	public DictEnum createEnum(String dictId, DictEnum dictEnum)
	{
		var dict = getById(dictId);

		var exists = dict.getEnums()
				.stream()
				.anyMatch(it -> it.getId().equals(dictEnum.getId()));

		if (exists)
		{
			throw new EnumAlreadyExistsException(dictEnum.getId());
		}

		return dictBackend.createEnum(dict, dictEnum);
	}

	public DictEnum updateEnum(String dictId, DictEnum dictEnum)
	{
		var dict = getById(dictId);

		var oldEnum = dict.getEnums()
				.stream()
				.filter(it -> it.getId().equals(dictEnum.getId()))
				.findAny()
				.orElseThrow(() -> new EnumNotFoundException(dictEnum.getId()));

		return dictBackend.updateEnum(dict, oldEnum, dictEnum);
	}

	public void deleteEnum(String dictId, String enumId)
	{
		var dict = getById(dictId);

		var exists = dict.getEnums()
				.stream()
				.anyMatch(it -> it.getId().equals(enumId));

		if (!exists)
		{
			throw new EnumNotFoundException(enumId);
		}

		dictBackend.deleteEnum(dict, enumId);
	}

	private Dict buildScheme(Dict source, Dict target)
	{
		addServiceFields(source.getFields());

		target.setId(source.getId());
		target.setName(source.getName());
		target.setFields(source.getFields());
		target.setViewPermission(source.getViewPermission());
		target.setEditPermission(source.getEditPermission());
		target.setDeleted(source.getDeleted());
		target.setIndexes(source.getIndexes());

		return target;
	}

	private void addServiceFields(List<DictField> dictFields)
	{
		dictFields.add(
				DictField.builder()
						.id(CREATED)
						.name("Дата создания")
						.type(DictFieldType.TIMESTAMP)
						.required(true)
						.multivalued(false)
						.build());

		dictFields.add(
				DictField.builder()
						.id(UPDATED)
						.name("Дата обновления")
						.type(DictFieldType.TIMESTAMP)
						.required(true)
						.multivalued(false)
						.build());

		dictFields.add(
				DictField.builder()
						.id(DELETED)
						.name("Дата удаления")
						.type(DictFieldType.TIMESTAMP)
						.required(false)
						.multivalued(false)
						.build());

		dictFields.add(
				DictField.builder()
						.id(HISTORY)
						.name("История изменений")
						.type(DictFieldType.JSON)
						.required(true)
						.multivalued(true)
						.build());

		dictFields.add(
				DictField.builder()
						.id(VERSION)
						.name("Версия")
						.type(DictFieldType.INTEGER)
						.required(true)
						.multivalued(false)
						.build());
	}
}
