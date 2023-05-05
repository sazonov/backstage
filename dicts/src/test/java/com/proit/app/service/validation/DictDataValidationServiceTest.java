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

package com.proit.app.service.validation;

import com.proit.app.common.AbstractValidationServiceTest;
import com.proit.app.domain.DictFieldName;
import com.proit.app.exception.*;
import com.proit.app.model.dictitem.DictDataItem;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DictDataValidationServiceTest extends AbstractValidationServiceTest
{
	@Test
	void validateSelectFieldsCorrect()
	{
		dictDataValidationService.validateSelectFields(REF_DICT_ID, List.of(new DictFieldName(DICT_ID, "integerField"),
				new DictFieldName(DICT_ID, "created"), new DictFieldName(DICT_ID, "stringField")));
	}

	@Test
	void validateSelectFieldsFieldNotExisted()
	{
		assertThrows(FieldNotFoundException.class,
				() -> dictDataValidationService.validateSelectFields(REF_DICT_ID, List.of(new DictFieldName(DICT_ID, "stringField"),
						new DictFieldName(DICT_ID, "incorrect"))));
	}

	@Test
	void validateSelectFieldsIncorrect()
	{
		assertThrows(UnavailableDictRefException.class, () ->
				dictDataValidationService.validateSelectFields(DICT_ID, List.of(new DictFieldName(DICT_ID, "integerField"))));
	}

	@Test
	void validateSelectFieldsDictNotExisted()
	{
		assertThrows(DictionaryNotFoundException.class, () -> dictDataValidationService.validateSelectFields("incorrect", List.of()));
	}

	@Test
	void validateDictDataCorrect()
	{
		Map<String, Object> stringDateMap = Map.of(
				"stringField", "string",
				"integerField", 1,
				"doubleField", Decimal128.parse("2.0"),
				"timestampField", "2021-08-15T06:00:00.000Z",
				"booleanField", true);

		Map<String, Object> objectDateMap = Map.of(
				"stringField", "string",
				"integerField", 1,
				"doubleField", Decimal128.parse("2.0"),
				"timestampField", new Date(),
				"booleanField", Boolean.TRUE);

		dictDataValidationService.validateDictDataItem(buildDictDataItem(DICT_ID, stringDateMap));
		dictDataValidationService.validateDictDataItem(buildDictDataItem(DICT_ID, objectDateMap));
	}

	@Test
	void validateDictDataDictNotExisted()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"integerField", 1,
				"timestampField", "2021-08-15T06:00:00.000Z");

		assertThrows(DictionaryNotFoundException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem("incorect", map)));
	}

	@Test
	void validateDictDataNoRequiredField()
	{
		Map<String, Object> map = Map.of("stringField", "string");

		var e = assertThrows(FieldValidationException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem(DICT_ID, map)));
		assertEquals(e.getMessage(), "Отсутствует обязательное поле: integerField.");
	}

	@Test
	void validateDictDataExistsField()
	{
		Map<String, Object> duplicatedFiled = Map.of(
				"stringField11", "text",
				"timestampField", "2021-08-15T06:00:00.000Z");

		assertThrows(ForbiddenFieldNameException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem(DICT_ID, duplicatedFiled)));
	}

	@Test
	void validateDictDataUnexpectedMultivalued()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"integerField", List.of(1, 2),
				"timestampField", "2021-08-15T06:00:00.000Z");

		var e = assertThrows(FieldValidationException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem(DICT_ID, map)));
		assertEquals(e.getMessage(), "Не может быть массивом: integerField.");
	}

	@Test
	void validateDictDataFieldCastException()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"integerField", 1,
				"doubleField", Decimal128.parse("2.0"),
				"timestampField", "incorrect");

		var e = assertThrows(FieldValidationException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem(DICT_ID, map)));
		assertEquals(e.getMessage(), "Некорректный формат данных: timestampField.");
	}

	@Test
	void validateDictDataForbiddenField()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"created", "incorrect",
				"integerField", 1,
				"timestampField", "2021-08-15T06:00:00.000Z");

		assertThrows(ForbiddenFieldNameException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem(DICT_ID, map)));
	}

	private DictDataItem buildDictDataItem(String dictId, Map<String, Object> dataMap)
	{
		return DictDataItem.of(dictId, dataMap);
	}
}
