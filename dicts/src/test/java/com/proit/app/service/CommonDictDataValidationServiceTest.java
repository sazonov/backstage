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

package com.proit.app.service;

import com.proit.app.common.CommonTest;
import com.proit.app.domain.DictField;
import com.proit.app.domain.DictFieldName;
import com.proit.app.domain.DictFieldType;
import com.proit.app.exception.dict.DictNotFoundException;
import com.proit.app.exception.dict.UnavailableDictRefException;
import com.proit.app.exception.dict.field.FieldNotFoundException;
import com.proit.app.exception.dict.field.FieldValidationException;
import com.proit.app.exception.dict.field.ForbiddenFieldNameException;
import com.proit.app.model.dictitem.DictDataItem;
import com.proit.app.service.validation.DictDataValidationService;
import com.proit.app.utils.SecurityUtils;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.proit.app.constant.ServiceFieldConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommonDictDataValidationServiceTest extends CommonTest
{
	protected static final String USER_ID = SecurityUtils.getCurrentUserId();

	protected static String TESTABLE_DICT_ID;
	protected static String TESTABLE_REF_DICT_ID;

	@Autowired
	protected DictDataValidationService dictDataValidationService;

	protected void buildDictDataTestableHierarchy(String storageDictId)
	{
		TESTABLE_DICT_ID = createNewDict(storageDictId + "dataValidate").getId();

		var refDict = buildDict(storageDictId + "dataValidateRef");

		var refDictFields = refDict.getFields();

		refDictFields.add(DictField.builder()
				.id(TESTABLE_DICT_ID)
				.name("Ссылка")
				.type(DictFieldType.DICT)
				.required(false)
				.multivalued(false)
				.dictRef(new DictFieldName(TESTABLE_DICT_ID, ID))
				.build()
		);

		TESTABLE_REF_DICT_ID = dictService.create(refDict).getId();

		addDictData(TESTABLE_DICT_ID);
		addDictData(TESTABLE_REF_DICT_ID);
	}

	protected void validateSelectFields()
	{
		dictDataValidationService.validateSelectFields(TESTABLE_REF_DICT_ID, List.of(new DictFieldName(TESTABLE_DICT_ID, "integerField"),
				new DictFieldName(TESTABLE_DICT_ID, "created"), new DictFieldName(TESTABLE_DICT_ID, "stringField")));
	}

	protected void validateSelectFieldsFieldNotExisted()
	{
		assertThrows(FieldNotFoundException.class,
				() -> dictDataValidationService.validateSelectFields(TESTABLE_REF_DICT_ID, List.of(new DictFieldName(TESTABLE_DICT_ID, "stringField"),
						new DictFieldName(TESTABLE_DICT_ID, "incorrect"))));
	}

	protected void validateSelectFieldsIncorrect()
	{
		assertThrows(UnavailableDictRefException.class, () ->
				dictDataValidationService.validateSelectFields(TESTABLE_DICT_ID, List.of(new DictFieldName(TESTABLE_DICT_ID, "integerField"))));
	}

	protected void validateSelectFieldsDictNotExisted()
	{
		assertThrows(DictNotFoundException.class, () -> dictDataValidationService.validateSelectFields("incorrect", List.of()));
	}

	protected void validateDictData()
	{
		Map<String, Object> stringDateMap = Map.of(
				"stringField", "string",
				"integerField", 1,
				"doubleField", BigDecimal.valueOf(Double.parseDouble("2.354")),
				"timestampField", "2021-08-15T06:00:00.000Z",
				"booleanField", true);

		Map<String, Object> objectDateMap = Map.of(
				"stringField", "string",
				"integerField", 1,
				"doubleField", BigDecimal.valueOf(Double.parseDouble("2.55532")),
				"timestampField", new Date(),
				"booleanField", Boolean.TRUE);

		dictDataValidationService.validateDictDataItem(buildDictDataItem(TESTABLE_DICT_ID, stringDateMap), USER_ID);
		dictDataValidationService.validateDictDataItem(buildDictDataItem(TESTABLE_DICT_ID, objectDateMap), USER_ID);
	}

	protected void validateDictDataDictNotExisted()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"integerField", 1,
				"timestampField", "2021-08-15T06:00:00.000Z");

		assertThrows(DictNotFoundException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem("incorect", map), USER_ID));
	}

	protected void validateDictDataNoRequiredField()
	{
		Map<String, Object> map = Map.of("stringField", "string");

		var e = assertThrows(FieldValidationException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem(TESTABLE_DICT_ID, map), USER_ID));
		assertEquals(e.getMessage(), "Отсутствует обязательное поле: integerField.");
	}

	protected void validateDictDataExistsField()
	{
		Map<String, Object> duplicatedFiled = Map.of(
				"stringField11", "text",
				"timestampField", "2021-08-15T06:00:00.000Z");

		assertThrows(ForbiddenFieldNameException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem(TESTABLE_DICT_ID, duplicatedFiled), USER_ID));
	}

	protected void validateDictDataUnexpectedMultivalued()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"integerField", List.of(1, 2),
				"timestampField", "2021-08-15T06:00:00.000Z");

		var e = assertThrows(FieldValidationException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem(TESTABLE_DICT_ID, map), USER_ID));
		assertEquals(e.getMessage(), "Не может быть массивом: integerField.");
	}

	protected void validateDictDataFieldCastException()
	{
		Map<String, Object> map = new HashMap<>(Map.of(
				"stringField", "string",
				"integerField", 1,
				"doubleField", BigDecimal.valueOf(Double.parseDouble("2.123")),
				"booleanField", false,
				"timestampField", "incorrect"));

		assertThrows(FieldValidationException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem(TESTABLE_DICT_ID, map), USER_ID));

		map.remove("timestampField");
		map.put("doubleField", Decimal128.parse("22.33"));

		assertThrows(FieldValidationException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem(TESTABLE_DICT_ID, map), USER_ID));

		map.put("doubleField", BigDecimal.valueOf(Double.parseDouble("2.123")));
		map.put("booleanField", "true");

		assertThrows(FieldValidationException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem(TESTABLE_DICT_ID, map), USER_ID));
	}

	protected void validateDictDataForbiddenField()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"created", "incorrect",
				"integerField", 1,
				"timestampField", "2021-08-15T06:00:00.000Z");

		assertThrows(ForbiddenFieldNameException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem(TESTABLE_DICT_ID, map), USER_ID));
	}

	private DictDataItem buildDictDataItem(String dictId, Map<String, Object> dataMap)
	{
		return DictDataItem.of(dictId, dataMap);
	}
}
