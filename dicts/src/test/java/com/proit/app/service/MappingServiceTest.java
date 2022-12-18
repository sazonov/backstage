package com.proit.app.service;

import com.proit.app.common.AbstractTest;
import com.proit.app.exception.DictionaryNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MappingServiceTest extends AbstractTest
{
	@Autowired private MappingService mappingService;

	@Test
	void mapDictDocCorrect()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"integerField", 1,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T07:00:00.000Z"),
				"doubleField", BigDecimal.valueOf(12.78));

		mappingService.mapDictDoc(DICT_ID, map);
	}

	@Test
	void mapDictDocDictNotExisted()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"integerField", 1,
				"timestampField", "2021-08-15T06:00:00.000Z");

		assertThrows(DictionaryNotFoundException.class, () -> mappingService.mapDictDoc("incorrect", map));
	}

	@Test
	void mapDictDateFieldsCorrect()
	{
		Map<String, Object> stringDateMap = Map.of("timestampField", "2021-08-15T06:00:00.000Z");
		Map<String, Object> objectDateMap = Map.of("timestampField", new Date());

		mappingService.mapDictDoc(DICT_ID, stringDateMap);
		mappingService.mapDictDoc(DICT_ID, objectDateMap);
	}

	@Test
	void mapDictItem()
	{
	}
}