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

import com.proit.app.dict.common.CommonTest;
import com.proit.app.dict.constant.ServiceFieldConstants;
import com.proit.app.dict.exception.dict.DictNotFoundException;
import com.proit.app.dict.model.dictitem.DictDataItem;
import com.proit.app.dict.service.mapping.DictFieldNameMappingService;
import com.proit.app.dict.service.mapping.DictItemMappingService;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommonMappingTest extends CommonTest
{
	protected static String TESTABLE_DICT_ID;

	@Autowired
	protected DictItemMappingService dictItemMappingService;

	@Autowired
	protected DictFieldNameMappingService dictFieldNameMappingService;

	protected void buildTestableDictHierarchy(String storageDictId)
	{
		TESTABLE_DICT_ID = createNewDict(storageDictId + "mapping").getId();

		addDictData(TESTABLE_DICT_ID);
	}

	protected void mapDictData()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"integerField", 1,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T07:00:00.000Z"),
				"doubleField", BigDecimal.valueOf(12.78));

		dictItemMappingService.mapDictItem(buildDictDataItem(TESTABLE_DICT_ID, map));
	}

	protected void mapDictDataDictNotExisted()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"integerField", 1,
				"timestampField", "2021-08-15T06:00:00.000Z");

		assertThrows(DictNotFoundException.class, () -> dictItemMappingService.mapDictItem(buildDictDataItem("incorrect", map)));
	}

	protected void mapDictDateFields()
	{
		Map<String, Object> stringDateMap = Map.of("timestampField", "2021-08-15T06:00:00.000Z");
		Map<String, Object> objectDateMap = Map.of("timestampField", new Date());

		dictItemMappingService.mapDictItem(buildDictDataItem(TESTABLE_DICT_ID, stringDateMap));
		dictItemMappingService.mapDictItem(buildDictDataItem(TESTABLE_DICT_ID, objectDateMap));
	}

	protected void mapDictFieldName()
	{
		var integerField = "integerField";
		var allField = "*";

		var integerFieldName = dictFieldNameMappingService.mapDictFieldName(integerField);
		var allFieldName = dictFieldNameMappingService.mapDictFieldName(allField);

		assertEquals("integerField", integerFieldName.getFieldId());
		assertEquals("*", allFieldName.getFieldId());
	}

	protected void mapServiceDictFieldName()
	{
		var serviceField = ServiceFieldConstants._ID;

		var actual = dictFieldNameMappingService.mapDictFieldName(serviceField);

		assertEquals(ServiceFieldConstants.ID, actual.getFieldId());
	}

	protected void mapInnerDictServiceDictFieldName()
	{
		var serviceField = TESTABLE_DICT_ID + "." + ServiceFieldConstants._ID;

		var actual = dictFieldNameMappingService.mapDictFieldName(serviceField);

		assertEquals(ServiceFieldConstants.ID, actual.getFieldId());
	}

	private DictDataItem buildDictDataItem(String dictId, Map<String, Object> dataItem)
	{
		return DictDataItem.of(dictId, dataItem);
	}
}
