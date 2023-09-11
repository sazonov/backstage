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

import com.proit.app.configuration.backend.provider.DictSchemeBackendProvider;
import com.proit.app.configuration.properties.DictsProperties;
import com.proit.app.constant.ServiceFieldConstants;
import com.proit.app.domain.*;
import com.proit.app.exception.dict.DictAlreadyExistsException;
import com.proit.app.exception.dict.DictUpdatedException;
import com.proit.app.exception.dict.constraint.ConstraintAlreadyExistsException;
import com.proit.app.exception.dict.constraint.ConstraintNotFoundException;
import com.proit.app.exception.dict.enums.EnumAlreadyExistsException;
import com.proit.app.exception.dict.enums.EnumNotFoundException;
import com.proit.app.exception.dict.field.FieldNotFoundException;
import com.proit.app.exception.dict.index.IndexAlreadyExistsException;
import com.proit.app.exception.dict.index.IndexNotFoundException;
import com.proit.app.service.backend.DictBackend;
import com.proit.app.service.backend.DictSchemeBackend;
import com.proit.app.service.validation.DictValidationService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.proit.app.constant.ServiceFieldConstants.*;

@Service
@RequiredArgsConstructor
public class DictService
{
	private final DictBackend dictBackend;

	private final DictSchemeBackendProvider schemeBackendProvider;

	private final DictValidationService dictValidationService;

	public Dict getById(String id)
	{
		return dictBackend.getDictById(id);
	}

	public List<Dict> getAll()
	{
		return dictBackend.getAllDicts();
	}

	public Dict create(Dict dict)
	{
		if (existsById(dict.getId()))
		{
			throw new DictAlreadyExistsException(dict.getId());
		}

		if (dict.getEngine() == null)
		{
			dict.setEngine(new DictEngine(DictsProperties.DEFAULT_ENGINE));
		}

		dictValidationService.validateDictScheme(dict);

		var created = buildScheme(dict, new Dict());

		schemeBackend(dict).createDictScheme(created);

		return dictBackend.saveDict(created);
	}

	//TODO: рассмотреть необходимость обновления схемы для DictConstraint/DictIndex
	// сейчас обновляется схема только для DictField и DictEnum, последний только в монго.
	public Dict update(String dictId, Dict dict)
	{
		var target = getById(dictId);

		//TODO: разработать обновление engine через api
		if (dict.getEngine() == null)
		{
			dict.setEngine(getById(dictId).getEngine());
		}

		Optional.ofNullable(target.getEngine())
				.map(DictEngine::getName)
				.filter(Predicate.not(dict.getEngine().getName()::equals))
				.ifPresent(it -> {
					throw new DictUpdatedException(dictId, "Обновление engine '%s' через API недоступно."
							.formatted(it));
				});

		dictValidationService.validateDictScheme(dict);

		var updated = buildScheme(dict, target);

		schemeBackend(dict).updateDictScheme(dictId, updated);

		var updatedFieldIds = updated.getFields()
				.stream()
				.map(DictField::getId)
				.collect(Collectors.toSet());

		var actualIndexes = updated.getIndexes()
				.stream()
				.filter(it -> updatedFieldIds.containsAll(it.getFields()))
				.toList();

		var actualConstraints = updated.getConstraints()
				.stream()
				.filter(it -> updatedFieldIds.containsAll(it.getFields()))
				.toList();

		updated.setIndexes(actualIndexes);
		updated.setConstraints(actualConstraints);

		if (!StringUtils.equals(dictId, updated.getId()))
		{
			dictBackend.deleteById(dictId);

			return dictBackend.saveDict(updated);
		}

		return dictBackend.updateDict(dictId, updated);
	}

	//	TODO: История изменений схемы, даты создания/обновления схемы?
	public void delete(String id, boolean deleted)
	{
		dictBackend.softDeleteById(id, deleted ? LocalDateTime.now() : null);
	}

	public boolean existsById(String id)
	{
		return dictBackend.existsById(id);
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

		var renamed = schemeBackend(dict).renameDictField(dict, fieldId, field);

		var actualIndexes = dict.getIndexes()
				.stream()
				.peek(it -> it.getFields().replaceAll(f -> StringUtils.equals(f, fieldId) ? renamed.getId() : f))
				.toList();

		var actualConstraints = dict.getConstraints()
				.stream()
				.peek(it -> it.getFields().replaceAll(f -> StringUtils.equals(f, fieldId) ? renamed.getId() : f))
				.toList();

		dict.setIndexes(actualIndexes);
		dict.setConstraints(actualConstraints);

		dictBackend.updateDict(dictId, dict);

		return renamed;
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

		var created = schemeBackend(dict).createConstraint(dict, constraint);

//		TODO: Валидация - ни один из адапатеров самостоятельно не добавил constraint
		dict.getConstraints().add(created);

		dictBackend.updateDict(dictId, dict);

		return created;
	}

	public void deleteConstraint(String dictId, String id)
	{
		var dict = getById(dictId);

		if (dict.getConstraints().stream().noneMatch(it -> it.getId().equals(id)))
		{
			throw new ConstraintNotFoundException(dictId, id);
		}

		schemeBackend(dict).deleteConstraint(dict, id);

//		TODO: Валидация - ни один из адапатеров самостоятельно не удалил constraint
		var actualConstraints = dict.getConstraints()
				.stream()
				.filter(it -> !StringUtils.equals(it.getId(), id))
				.collect(Collectors.toList());

		dict.setConstraints(actualConstraints);

		dictBackend.updateDict(dictId, dict);
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

		var created = schemeBackend(dict).createIndex(dict, index);

//		TODO: Валидация - ни один из адапатеров самостоятельно не добавил index
		dict.getIndexes().add(created);

		dictBackend.updateDict(dictId, dict);

		return created;
	}

	public void deleteIndex(String dictId, String id)
	{
		var dict = getById(dictId);

		if (dict.getIndexes().stream().noneMatch(it -> it.getId().equals(id)))
		{
			throw new IndexNotFoundException(dictId, id);
		}

		schemeBackend(dict).deleteIndex(dict, id);

//		TODO: Валидация - ни один из адапатеров самостоятельно не удалил index
		var actualIndexes = dict.getIndexes()
				.stream()
				.filter(it -> !StringUtils.equals(it.getId(), id))
				.collect(Collectors.toList());

		dict.setIndexes(actualIndexes);

		dictBackend.updateDict(dictId, dict);
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

//		TODO: Валидация - ни один из адапатеров самостоятельно не добавил enum
		dict.getEnums().add(dictEnum);

		return dictBackend.createEnum(dict, dictEnum);
	}

	public DictEnum updateEnum(String dictId, DictEnum dictEnum)
	{
		var dict = getById(dictId);

		return dictBackend.updateEnum(dict, dictEnum);
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

//		TODO: Валидация - ни один из адапатеров самостоятельно не удалил enum
		var actualEnums = dict.getEnums()
				.stream()
				.filter(it -> !StringUtils.equals(it.getId(), enumId))
				.toList();

		dict.setEnums(actualEnums);

		dictBackend.deleteEnum(dict, enumId);
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
//				FIXME: На данном этапе коллекция должна быть изменяемой
				.collect(Collectors.toList());
	}

	public Map<String, DictField> getReferenceFieldMap(String id)
	{
		return getById(id).getFields()
				.stream()
				.filter(field -> DictFieldType.DICT.equals(field.getType()))
				.collect(Collectors.toMap(DictField::getId, Function.identity()));
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
		target.setEnums(source.getEnums());
		target.setConstraints(source.getConstraints());
		target.setEngine(source.getEngine());

		return target;
	}

	private void addServiceFields(List<DictField> dictFields)
	{
		dictFields.add(0, DictField.builder()
				.id(ID)
				.name("Идентификатор")
				.type(DictFieldType.STRING)
				.required(false)
				.multivalued(false)
				.build());

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
						.id(DELETION_REASON)
						.name("Причина удаления")
						.type(DictFieldType.STRING)
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

	private DictSchemeBackend schemeBackend(Dict dict)
	{
		return schemeBackendProvider.getBackendByEngineName(dict.getEngine().getName());
	}
}
