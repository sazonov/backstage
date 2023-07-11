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

package com.proit.app.service.mapping;

import com.proit.app.common.AbstractTest;
import com.proit.app.constant.ServiceFieldConstants;
import com.proit.app.exception.dictionary.DictNotFoundException;
import com.proit.app.model.dictitem.DictDataItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MappingTest extends AbstractTest
{
	@Autowired
	private DictItemMappingService dictItemMappingService;

	@Autowired
	private DictFieldNameMappingService dictFieldNameMappingService;

	@Test
	void mapDictDataCorrect()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"integerField", 1,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T07:00:00.000Z"),
				"doubleField", BigDecimal.valueOf(12.78));

		dictItemMappingService.mapDictItem(buildDictDataItem(DICT_ID, map));
	}

	@Test
	void mapDictDataDictNotExisted()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"integerField", 1,
				"timestampField", "2021-08-15T06:00:00.000Z");

		assertThrows(DictNotFoundException.class, () -> dictItemMappingService.mapDictItem(buildDictDataItem("incorrect", map)));
	}

	@Test
	void mapDictDateFieldsCorrect()
	{
		Map<String, Object> stringDateMap = Map.of("timestampField", "2021-08-15T06:00:00.000Z");
		Map<String, Object> objectDateMap = Map.of("timestampField", new Date());

		dictItemMappingService.mapDictItem(buildDictDataItem(DICT_ID, stringDateMap));
		dictItemMappingService.mapDictItem(buildDictDataItem(DICT_ID, objectDateMap));
	}

	@Test
	void map_dictFieldNameCorrect()
	{
		var integerField = "integerField";
		var allField = "*";

		var integerFieldName = dictFieldNameMappingService.mapDictFieldName(integerField);
		var allFieldName = dictFieldNameMappingService.mapDictFieldName(allField);

		assertEquals("integerField", integerFieldName.getFieldId());
		assertEquals("*", allFieldName.getFieldId());
	}

	//TODO: При реализации валидации переданных клиентом сервисных для адаптера полей, актуализировать тест.
	@Deprecated(forRemoval = true)
	@Test
	void map_serviceDictFieldNameCorrect()
	{
		var serviceField = ServiceFieldConstants._ID;

		var actual = dictFieldNameMappingService.mapDictFieldName(serviceField);

		assertEquals(ServiceFieldConstants.ID, actual.getFieldId());
	}

	//TODO: При реализации валидации переданных клиентом сервисных для адаптера полей, актуализировать тест.
	@Deprecated(forRemoval = true)
	@Test
	void map_innerDictServiceDictFieldNameCorrect()
	{
		var serviceField = DICT_ID + "." + ServiceFieldConstants._ID;

		var actual = dictFieldNameMappingService.mapDictFieldName(serviceField);

		assertEquals(ServiceFieldConstants.ID, actual.getFieldId());
	}

	private DictDataItem buildDictDataItem(String dictId, Map<String, Object> dataItem)
	{
		return DictDataItem.of(dictId, dataItem);
	}
}