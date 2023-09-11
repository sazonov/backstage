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

import com.proit.app.conversion.dto.data.DictItemConverter;
import com.proit.app.domain.DictItem;
import com.proit.app.model.dto.data.DictItemDto;
import lombok.SneakyThrows;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CommonDictDataConversionTest extends CommonDictDataServiceTest
{
	@Autowired
	private DictItemConverter itemConverter;

	protected void convertItemsWithNullData()
	{
		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("doubleField", null);

		dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, dataMap));

		var actualDto = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), "doubleField = null", Pageable.unpaged())
				.getContent()
				.stream()
				.map(this::mappedDto)
				.toList();

		actualDto.forEach(it -> assertNull(it.getData().get("doubleField")));
	}

	protected void convertItemsWithEmptyArray()
	{
		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("timestampField", null);

		dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, dataMap));

		var actualDto = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), null, Pageable.unpaged())
				.getContent()
				.stream()
				.filter(it -> ((Collection<?>) it.getData().get("timestampField")).isEmpty())
				.map(this::mappedDto)
				.toList();

		actualDto.forEach(it -> assertTrue(((Collection<?>) it.getData().get("timestampField")).isEmpty()));
	}

	protected void convertItemsWithReferenceDict()
	{
		var refId = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP)).getId();

		var refDataMap = new HashMap<>(DATA_MAP);
		refDataMap.put(TESTABLE_DICT_ID, refId);

		dictDataService.create(buildDictDataItem(TESTABLE_REF_DICT_ID, refDataMap));

		var actualDto = dictDataService.getByFilter(TESTABLE_REF_DICT_ID, List.of("*", TESTABLE_DICT_ID + ".timestampField"), null, PageRequest.of(0, 10))
				.getContent()
				.stream()
				.map(this::mappedDto)
				.toList();

		assertEquals(DictItem.class, actualDto.get(0).getData().get(TESTABLE_DICT_ID).getClass());
	}

	@SneakyThrows
	protected void convertItemsWithGeoJsonObject()
	{
		var geo = objectMapper.readValue("{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Point\",\"coordinates\":[37.412284,55.603515]}},{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[37.413423,55.604283],[37.41255,55.60361],[37.413995,55.602974],[37.414842,55.603629],[37.413423,55.604283]]]}},{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[37.411999,55.603159],[37.413568,55.602384],[37.41522,55.603516]]}}]}",
				GeoJsonObject.class);
		var geoJson = objectMapper.writeValueAsString(geo);

		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("geoJsonField", geoJson);
		dataMap.put("geoJsonMultivaluedField", List.of(geoJson, geoJson));
		dataMap.put("stringField", "geoJsonTest");

		dictDataService.create(buildDictDataItem(TESTABLE_GEO_JSON_DICT_ID, dataMap));

		var actualDto = dictDataService.getByFilter(TESTABLE_GEO_JSON_DICT_ID, List.of("*"), "stringField = 'geoJsonTest'", Pageable.unpaged())
				.getContent()
				.stream()
				.map(this::mappedDto)
				.toList();

		assertEquals(FeatureCollection.class, actualDto.get(0).getData().get("geoJsonField").getClass());
	}

	@SneakyThrows
	private DictItemDto mappedDto(DictItem dictItem)
	{
		return objectMapper.readValue(objectMapper.writeValueAsString(itemConverter.convert(dictItem)), DictItemDto.class);
	}
}
