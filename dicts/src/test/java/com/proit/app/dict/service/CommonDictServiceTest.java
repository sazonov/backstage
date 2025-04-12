/*
 *
 *  Copyright 2019-2023 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.proit.app.dict.service;

import com.proit.app.dict.api.domain.DictFieldType;
import com.proit.app.dict.common.CommonTest;
import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.domain.DictConstraint;
import com.proit.app.dict.domain.DictField;
import com.proit.app.dict.domain.DictIndex;
import com.proit.app.dict.domain.DictEngine;
import com.proit.app.dict.exception.dict.DictAlreadyExistsException;
import com.proit.app.dict.exception.dict.DictDeletedException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.proit.app.dict.constant.ServiceFieldConstants.*;
import static org.junit.jupiter.api.Assertions.*;

public class CommonDictServiceTest extends CommonTest
{
	protected void getDictById(String dictId)
	{
		var expected = createNewDict(dictId);

		assertEquals(expected, dictService.getById(expected.getId()));
	}

	protected void getByIdDeletedDict(String dictId)
	{
		var deleted = createNewDict(dictId);

		dictService.delete(deleted.getId(), true);

		assertThrows(DictDeletedException.class, () -> dictService.getById(deleted.getId()));
	}

	protected void createDict(String dictId)
	{
		var expected = buildDict(dictId);

		var actual = dictService.create(expected);

		assertEquals(dictService.getById(expected.getId()), actual);

		var existsCorrectedServiceFields = actual.getFields().stream().map(DictField::getId).anyMatch(it -> it.equals(ID));
		var existsUncorrectedServiceFields = actual.getFields().stream().map(DictField::getId).anyMatch(it -> it.equals(_ID));

		assertTrue(existsCorrectedServiceFields);
		assertFalse(existsUncorrectedServiceFields);

		dictService.delete(expected.getId(), true);
	}

	protected void updateDict(String dictId)
	{
		var updateDict = createNewDict(dictId);

		updateDict.setName("updated_" + updateDict.getId());

		updateDict.setIndexes(List.of(buildIndex(dictId, "stringField", "integerField"), buildIndex(dictId, "doubleField")));

		var fields = new ArrayList<>(withoutServiceFields(updateDict.getFields()));

		var field = DictField.builder()
				.id("field_" + updateDict.getId())
				.name("name")
				.required(false)
				.multivalued(false)
				.type(DictFieldType.STRING)
				.maxSize(10)
				.maxSize(20)
				.build();

		fields.add(field);

		fields.removeIf(it -> StringUtils.equals(it.getId(), "integerField"));

		updateDict.setFields(fields);

		var expected = dictService.update(updateDict.getId(), updateDict);

		var actual = dictService.getById(updateDict.getId());

		assertEquals(expected, actual);

		var noneExistsDroppedFieldsIndex = actual.getIndexes()
				.stream()
				.map(DictIndex::getFields)
				.flatMap(Collection::stream)
				.allMatch(it -> StringUtils.equals(it, "doubleField") && !StringUtils.equalsAny("stringField", "integerField"));

		var noneExistsDroppedFieldsConstraint = actual.getConstraints()
				.stream()
				.map(DictConstraint::getFields)
				.flatMap(Collection::stream)
				.noneMatch(it -> StringUtils.equalsAny(it, "stringField", "integerField"));

		assertTrue(noneExistsDroppedFieldsIndex);
		assertTrue(noneExistsDroppedFieldsConstraint);

		dictService.delete(updateDict.getId(), true);
	}

	protected void renameDictField(String dictId)
	{
		var d = buildDict(dictId);

		d.getConstraints().add(buildConstraint(dictId, "stringField"));

		dictService.create(d);

		var renamed = d.getFields()
				.stream()
				.filter(it -> StringUtils.equals(it.getId(), "stringField"))
				.peek(it -> {
					it.setId("renamed_" + it.getId());
					it.setName("renamed_" + it.getName());
				})
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Rename DictField test failed."));

		var actualField = dictService.renameField(d.getId(), "stringField", renamed.getId(), renamed.getName());
		var actualDict = dictService.getById(d.getId());

		var successRenameFieldIndex = actualDict.getIndexes()
				.stream()
				.map(DictIndex::getFields)
				.flatMap(Collection::stream)
				.allMatch(it -> !StringUtils.equals(it, "stringField") && StringUtils.containsAny(it, renamed.getId()));

		var successRenameFieldConstraint = actualDict.getConstraints()
				.stream()
				.map(DictConstraint::getFields)
				.flatMap(Collection::stream)
				.allMatch(it -> !StringUtils.equals(it, "stringField") && StringUtils.containsAny(it, renamed.getId()));

		assertEquals(renamed, actualField);

		assertTrue(successRenameFieldIndex);
		assertTrue(successRenameFieldConstraint);

		dictService.delete(actualDict.getId(), true);
	}

	protected void createAlreadyExistsDict(String dictId)
	{
		var d = createNewDict(dictId);

		var alreadyExistsDict = Dict.builder().id(d.getId()).build();

		assertThrows(DictAlreadyExistsException.class, () -> dictService.create(alreadyExistsDict));

		dictService.delete(d.getId(), true);
	}

	protected void createWithUUIDDictFieldIds()
	{
		var dict = buildDict("the_uuid_has_a_digit_as_the_first_bit");

		dict.setId(RandomStringUtils.randomNumeric(1) + StringUtils.substring(UUID.randomUUID().toString(), 1));
		dict.setFields(dict.getFields()
				.stream()
				.peek(it -> it.setId(RandomStringUtils.randomNumeric(1) + StringUtils.substring(generateRandomUUIDWithoutDashes(), 1)))
				.collect(Collectors.toList()));

		var expected = dictService.create(dict);

		assertEquals(expected, dictService.getById(dict.getId()));
	}

	protected void deleteDict(String dictId)
	{
		var expected = createNewDict(dictId);

		dictService.delete(expected.getId(), true);

		assertThrows(DictDeletedException.class, () -> dictService.getById(expected.getId()));
	}

	protected void deleteDictWithAnotherEngine(String dictId, DictEngine engine)
	{
		var expected = createNewDict(dictId, engine);

		dictService.delete(expected.getId(), true);

		assertThrows(DictDeletedException.class, () -> dictService.getById(expected.getId()));
	}

	protected void restoreDeletedDict(String dictId)
	{
		var dict = createNewDict(dictId);

		dictService.delete(dict.getId(), true);

		dictService.delete(dict.getId(), false);

		var restoredDict = dictService.getById(dict.getId());

		assertNull(restoredDict.getDeleted());
	}

	protected void createDictIndex(String dictId)
	{
		var expectedDict = createNewDict(dictId);

		var expectedSize = dictService.getById(expectedDict.getId()).getIndexes().size() + 1;

		var indexes = new ArrayList<>(expectedDict.getIndexes());

		indexes.add(dictService.createIndex(expectedDict.getId(), buildIndex(expectedDict.getId(), "timestampField")));

		expectedDict.setIndexes(indexes);

		var actualDict = dictService.getById(expectedDict.getId());

		assertEquals(expectedDict, actualDict);
		assertEquals(expectedSize, actualDict.getIndexes().size());

		dictService.delete(expectedDict.getId(), true);
	}

	protected void deleteDictIndex(String dictId)
	{
		var expectedDict = createNewDict(dictId);

		var index = dictService.createIndex(expectedDict.getId(), buildIndex(expectedDict.getId(), "doubleField"));

		var expectedSize = dictService.getById(expectedDict.getId()).getIndexes().size() - 1;

		dictService.deleteIndex(expectedDict.getId(), index.getId());

		var actualDict = dictService.getById(expectedDict.getId());

		assertEquals(expectedDict, actualDict);
		assertEquals(expectedSize, actualDict.getIndexes().size());

		dictService.delete(expectedDict.getId(), true);
	}

	protected void createDictConstraint(String dictId)
	{
		var expectedDict = createNewDict(dictId);

		var expectedSize = dictService.getById(expectedDict.getId()).getConstraints().size() + 1;

		var constraints = new ArrayList<>(expectedDict.getConstraints());
		constraints.add(dictService.createConstraint(expectedDict.getId(), buildConstraint(expectedDict.getId(), "stringField")));

		expectedDict.setConstraints(constraints);

		var actualDict = dictService.getById(expectedDict.getId());

		assertEquals(expectedDict, actualDict);
		assertEquals(expectedSize, actualDict.getConstraints().size());

		dictService.delete(expectedDict.getId(), true);
	}

	protected void deleteDictConstraint(String dictId)
	{
		var expectedDict = createNewDict(dictId);

		var constraint = dictService.createConstraint(expectedDict.getId(), buildConstraint(expectedDict.getId(), "integerField"));

		var expectedSize = dictService.getById(expectedDict.getId()).getConstraints().size() - 1;

		dictService.deleteConstraint(expectedDict.getId(), constraint.getId());

		var actualDict = dictService.getById(expectedDict.getId());

		assertEquals(expectedDict, actualDict);
		assertEquals(expectedSize, actualDict.getConstraints().size());

		dictService.delete(expectedDict.getId(), true);
	}

	protected void createDictEnum(String dictId)
	{
		var expectedDict = createNewDict(dictId);

		var expectedSize = dictService.getById(expectedDict.getId()).getEnums().size() + 1;

		var enums = new ArrayList<>(expectedDict.getEnums());
		enums.add(dictService.createEnum(expectedDict.getId(), buildEnum(expectedDict.getId())));

		expectedDict.setEnums(enums);

		var actualDict = dictService.getById(expectedDict.getId());

		assertEquals(expectedDict, actualDict);
		assertEquals(expectedSize, actualDict.getEnums().size());

		dictService.delete(expectedDict.getId(), true);
	}

	protected void updateDictEnum(String dictId)
	{
		var expectedDict = createNewDict(dictId);

		var enums = new ArrayList<>(expectedDict.getEnums());
		var dictEnum = dictService.createEnum(expectedDict.getId(), buildEnum(expectedDict.getId()));
		enums.add(dictEnum);

		expectedDict.setEnums(enums);

		dictEnum.setName("updated_" + dictEnum.getName());

		var actual = dictService.updateEnum(expectedDict.getId(), dictEnum);

		assertEquals(expectedDict, dictService.getById(expectedDict.getId()));
		assertEquals(dictEnum.getName(), actual.getName());

		dictService.delete(expectedDict.getId(), true);
	}

	protected void deleteDictEnum(String dictId)
	{
		var expectedDict = createNewDict(dictId);

		var dictEnum = dictService.createEnum(expectedDict.getId(), buildEnum(expectedDict.getId()));

		var expectedSize = dictService.getById(expectedDict.getId()).getEnums().size() - 1;

		dictService.deleteEnum(expectedDict.getId(), dictEnum.getId());

		var actualDict = dictService.getById(expectedDict.getId());

		assertEquals(expectedDict, actualDict);
		assertEquals(expectedSize, actualDict.getEnums().size());

		dictService.delete(expectedDict.getId(), true);
	}
}
