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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableMap;
import com.proit.app.dict.common.CommonTest;
import com.proit.app.dict.constant.ServiceFieldConstants;
import com.proit.app.dict.domain.DictField;
import com.proit.app.dict.domain.DictFieldName;
import com.proit.app.dict.api.domain.DictFieldType;
import com.proit.app.dict.domain.DictItem;
import com.proit.app.exception.ObjectNotFoundException;
import com.proit.app.dict.exception.dict.DictConcurrentUpdateException;
import com.proit.app.dict.exception.dict.field.FieldNotFoundException;
import com.proit.app.dict.model.dictitem.DictDataItem;
import com.proit.app.attachment.model.domain.Attachment;
import com.proit.app.model.other.date.DateConstants;
import com.proit.app.model.other.user.UserInfo;
import com.proit.app.attachment.service.AttachmentService;
import com.proit.app.utils.StreamCollectors;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.ThrowingConsumer;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.proit.app.dict.constant.ServiceFieldConstants.ID;
import static com.proit.app.dict.service.advice.BindingDictDataServiceAdvice.DICTS_ATTACHMENT_TYPE_TEMPLATE;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommonDictDataServiceTest extends CommonTest
{
	protected static String TESTABLE_DICT_ID;
	protected static String TESTABLE_REF_DICT_ID;
	protected static String TESTABLE_JSON_DICT_ID;
	protected static String TESTABLE_ATTACH_DICT_ID;
	protected static String TESTABLE_GEO_JSON_DICT_ID;

	@Autowired
	private AttachmentService attachmentService;

	@Autowired
	private ObjectMapper mapper;

	@Value("classpath:attachment.png")
	protected Resource firstFileResource;

	@Value("classpath:attachment2.png")
	protected Resource secondFileResource;

	@Value("classpath:attachment3.png")
	protected Resource thirdFileResource;

	protected String firstAttachmentId;
	protected String secondAttachmentId;
	protected String thirdAttachmentId;

	protected Attachment firstAttachment;
	protected Attachment secondAttachment;
	protected Attachment thirdAttachment;

	protected static final Map<String, Object> DATA_MAP = Map.of(
			"stringField", "string",
			"integerField", 1,
			"doubleField", 2.558,
			"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
			"booleanField", true);

	protected void initDictDataTestableHierarchy(String storageDictId)
	{
		TESTABLE_DICT_ID = createNewDict(storageDictId + "data").getId();

		var refDict = buildDict(storageDictId + "dataRef");

		refDict.getFields()
				.add(DictField.builder()
						.id(TESTABLE_DICT_ID)
						.name("Ссылка")
						.type(DictFieldType.DICT)
						.required(false)
						.multivalued(false)
						.dictRef(new DictFieldName(TESTABLE_DICT_ID, ID))
						.build());

		TESTABLE_REF_DICT_ID = dictService.create(refDict).getId();

		var attachDict = buildDict(storageDictId + "dataAttach");

		attachDict.getFields()
				.add(DictField.builder()
						.id("attachmentField")
						.name("Вложение")
						.type(DictFieldType.ATTACHMENT)
						.required(false)
						.multivalued(false)
						.build());

		attachDict.getFields()
				.add(DictField.builder()
						.id("attachmentsField")
						.name("Вложения")
						.type(DictFieldType.ATTACHMENT)
						.required(false)
						.multivalued(true)
						.build());

		TESTABLE_ATTACH_DICT_ID = dictService.create(attachDict).getId();

		var jsonDict = buildDict(storageDictId + "data_json");

		jsonDict.getFields()
				.add(DictField.builder()
						.id("jsonField")
						.name("Json")
						.type(DictFieldType.JSON)
						.required(true)
						.multivalued(false)
						.build());

		jsonDict.getFields()
				.add(DictField.builder()
						.id("jsonMultivaluedField")
						.name("Массив Json")
						.type(DictFieldType.JSON)
						.required(false)
						.multivalued(true)
						.build());

		TESTABLE_JSON_DICT_ID = dictService.create(jsonDict).getId();

		var geoJsonDict = buildDict(storageDictId + "data_geo_json");

		geoJsonDict.getFields()
				.add(DictField.builder()
						.id("geoJsonField")
						.name("GeoJson")
						.type(DictFieldType.GEO_JSON)
						.required(true)
						.multivalued(false)
						.build());

		geoJsonDict.getFields()
				.add(DictField.builder()
						.id("geoJsonMultivaluedField")
						.name("Массив GeoJson")
						.type(DictFieldType.GEO_JSON)
						.required(false)
						.multivalued(true)
						.build());

		TESTABLE_GEO_JSON_DICT_ID = dictService.create(geoJsonDict).getId();
	}

	@BeforeAll
	public void setupAttachment() throws IOException
	{
		firstAttachment = createAttachment(firstFileResource);
		secondAttachment = createAttachment(secondFileResource);
		thirdAttachment = createAttachment(thirdFileResource);

		firstAttachmentId = firstAttachment.getId();
		secondAttachmentId = secondAttachment.getId();
		thirdAttachmentId = thirdAttachment.getId();
	}

	private final ThrowingConsumer<Object> objectWriter = obj -> System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj));

	protected void getByIds()
	{
		var ids = dictDataService.createMany(TESTABLE_DICT_ID, List.of(DATA_MAP, DATA_MAP))
				.stream()
				.map(DictItem::getId)
				.toList();

		assertArrayEquals(ids.toArray(String[]::new), dictDataService.getByIds(TESTABLE_DICT_ID, ids).stream().map(DictItem::getId).toArray(String[]::new));
	}

	protected void getByFilterWithNullRefField()
	{
		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("stringField", "nullableDictRefField");

		var createdItemId = dictDataService.create(buildDictDataItem(TESTABLE_REF_DICT_ID, dataMap))
				.getId();

		var result = dictDataService.getByFilter(TESTABLE_REF_DICT_ID, List.of("*", TESTABLE_DICT_ID + ".*"), "stringField = 'nullableDictRefField'", PageRequest.of(0, 10));

		assertEquals(result.getContent().size(), 1);
		assertEquals(result.getContent().get(0).getId(), createdItemId);
	}

	protected void getByFilter()
	{
		var result = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"),
				"stringField like 'str' and (integerField = 1 or integerField in (2, 5, 8) and integerField != 10 or integerField <= 2 and doubleField > 1.9 and doubleField < 2.1) and stringField != 'stringFieldLogicalExpressionTest'",
				PageRequest.of(0, 10));

		var allMatchStringField = result.getContent()
				.stream()
				.map(DictItem::getData)
				.map(Map::entrySet)
				.flatMap(Collection::stream)
				.filter(it -> "stringField".equals(it.getKey()))
				.map(Map.Entry::getValue)
				.allMatch(DATA_MAP.get("stringField")::equals);

		assertTrue(allMatchStringField);
	}

	protected void getByFilterWithLogicalExpression()
	{
		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("stringField", "stringFieldLogicalExpressionTest");

		var dictItemId = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, dataMap))
				.getId();

		var result = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"),
				"stringField = 'stringFieldLogicalExpressionTest' and (doubleField = 2.558 or integerField = 1)",
				PageRequest.of(0, 10));

		assertEquals(result.getContent().size(), 1);
		assertEquals(result.getContent().get(0).getId(), dictItemId);
	}

	protected void getIdsByFilter()
	{
		var result = dictDataService.getIdsByFilter(TESTABLE_DICT_ID, "integerField = 1 or stringField like 'str' or integerField in (2, 5, 8) and integerField != 10 or integerField <= 2 and doubleField > 1.9 and doubleField < 2.1");

		assertNotNull(result.getContent().get(0));
	}

	//TODO: тест - с ambiguous состоянием указанных поле в сортировке
	protected void getByFilterInnerDictSort()
	{
		final String integerField = createDictHierarchy();

		var result = dictDataService.getByFilter(TESTABLE_REF_DICT_ID, List.of("*", TESTABLE_DICT_ID + ".*"), "%s != null".formatted(TESTABLE_DICT_ID),
						PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, TESTABLE_DICT_ID + "." + integerField)))
				.getContent()
				.stream()
				.map(DictItem::getData)
				.toList();

		objectWriter.accept(result);

		boolean actual = Comparators.isInOrder(result, Comparator.comparing((Map<String, Object> data) -> (Long) ((DictItem) data.get(TESTABLE_DICT_ID)).getData().get(integerField)).reversed());

		assertTrue(actual);
	}

	protected void getByFilterInnerDictSortWrongFiledName()
	{
		assertThrows(FieldNotFoundException.class, () -> dictDataService.getByFilter(TESTABLE_REF_DICT_ID, List.of("*", TESTABLE_DICT_ID + ".*"), null,
				PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, TESTABLE_DICT_ID + "." + "wrongFiled"))));
	}

	protected void getByFilterDictSortServiceField()
	{
		createDictHierarchy();

		var sortedIdFields = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), null,
						PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, ServiceFieldConstants.ID)))
				.getContent()
				.stream()
				.map(DictItem::getId)
				.toList();

		var sortedCreatedFields = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), null,
						PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, ServiceFieldConstants.CREATED)))
				.getContent()
				.stream()
				.map(DictItem::getCreated)
				.toList();

		var actualIdFields = Comparators.isInOrder(sortedIdFields, Comparator.reverseOrder());
		var actualCreatedFields = Comparators.isInOrder(sortedCreatedFields, Comparator.reverseOrder());

		assertTrue(actualIdFields);
		assertTrue(actualCreatedFields);
	}

	protected void getByFilterDictSortDataField()
	{
		var sortedDataField = createDictHierarchy();

		var result = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), null,
						PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, sortedDataField)))
				.getContent()
				.stream()
				.map(DictItem::getData)
				.map(map -> (Long) map.get(sortedDataField))
				.toList();

		var actual = Comparators.isInOrder(result, Comparator.reverseOrder());

		assertTrue(actual);
	}

	protected void getByFilterWithServiceSelectField()
	{
		createDictHierarchy();

		var result = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of(ServiceFieldConstants._ID, ServiceFieldConstants.CREATED),
						null, PageRequest.of(0, 20))
				.getContent()
				.stream()
				.map(it -> StringUtils.hasText(it.getId()) && it.getCreated() != null)
				.toList();

		result.forEach(Assertions::assertTrue);
	}

	protected void getByFilterWithDifferentDateCorrect()
	{
		var localDateTime = LocalDateTime.of(2021, 8, 15, 6, 0, 0);
		var date = Date.from(LocalDateTime.of(2021, 8, 15, 6, 0, 0).atZone(ZoneId.systemDefault()).toInstant());

		Map<String, Object> stringLocalDateTimeMap = Map.of(
				"stringField", "string",
				"integerField", 1.0,
				"doubleField", 2.50,
				"timestampField", "2021-08-15T06:00:00.000Z",
				"booleanField", Boolean.TRUE);

		Map<String, Object> objectLocalDateTimeMap = Map.of(
				"stringField", "string",
				"integerField", 1.0,
				"doubleField", 2.50,
				"timestampField", localDateTime,
				"booleanField", Boolean.FALSE);

		Map<String, Object> stringDateDataMap = Map.of(
				"stringField", "string",
				"integerField", 1.0,
				"doubleField", 2.50,
				"timestampField", "2021-08-15T06:00:00.000Z",
				"booleanField", Boolean.TRUE);

		Map<String, Object> objectDateDataMap = Map.of(
				"stringField", "string",
				"integerField", 1.0,
				"doubleField", 2.50,
				"timestampField", date,
				"booleanField", Boolean.FALSE);

		var stringDateItemId = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, stringDateDataMap)).getId();
		var objectDateItemId = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, objectDateDataMap)).getId();
		var stringLocalDateTimeItemId = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, stringLocalDateTimeMap)).getId();
		var objectLocalDateTimeItemId = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, objectLocalDateTimeMap)).getId();

		var stringDateItem = dictDataService.getById(TESTABLE_DICT_ID, stringDateItemId);
		var objectDateItem = dictDataService.getById(TESTABLE_DICT_ID, objectDateItemId);
		var stringLocalDateTimeItem = dictDataService.getById(TESTABLE_DICT_ID, stringLocalDateTimeItemId);
		var objectLocalDateTimeItem = dictDataService.getById(TESTABLE_DICT_ID, objectLocalDateTimeItemId);

		assertEquals(stringDateItem.getData().get("timestampField"), List.of(localDateTime));
		assertEquals(objectDateItem.getData().get("timestampField"), List.of(localDateTime));
		assertEquals(stringLocalDateTimeItem.getData().get("timestampField"), List.of(localDateTime));
		assertEquals(objectLocalDateTimeItem.getData().get("timestampField"), List.of(localDateTime));
	}

	//TODO: тест с выбором всех полей у refDict
	protected void getByFilterWithDictReference()
	{
		var refId = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP)).getId();

		var refDataMap = new HashMap<>(DATA_MAP);
		refDataMap.put(TESTABLE_DICT_ID, refId);

		dictDataService.create(buildDictDataItem(TESTABLE_REF_DICT_ID, refDataMap));

		var result = dictDataService.getByFilter(TESTABLE_REF_DICT_ID, List.of("*", TESTABLE_DICT_ID + ".timestampField"), null, PageRequest.of(0, 10));

		assertNotNull(result.getContent().get(0).getData().get(TESTABLE_DICT_ID));
		assertEquals(DictItem.class, result.getContent().get(0).getData().get(TESTABLE_DICT_ID).getClass());
		assertTrue(((DictItem) result.getContent().get(0).getData().get(TESTABLE_DICT_ID)).getData().containsKey("timestampField"));
	}

	protected void getByFilterWithDictReferenceAllFieldSelect()
	{
		var refId = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP)).getId();

		var refDataMap = new HashMap<>(DATA_MAP);
		refDataMap.put(TESTABLE_DICT_ID, refId);

		var createdDitDataItemId = dictDataService.create(buildDictDataItem(TESTABLE_REF_DICT_ID, refDataMap))
				.getId();

		var result = dictDataService.getByFilter(TESTABLE_REF_DICT_ID, List.of("*"), "%s = '%s'".formatted(TESTABLE_DICT_ID, refId), PageRequest.of(0, 10))
				.getContent();

		assertEquals(result.size(), 1);
		assertEquals(result.get(0).getId(), createdDitDataItemId);
		assertEquals(result.get(0).getData().size(), refDataMap.size());
		assertEquals(result.get(0).getData().get(TESTABLE_DICT_ID), refId);
	}

	//TODO: тест с фильтрацией reference Dict по элементам массива
	protected void getByFilterWithQueryReference()
	{
		var refDictDataMap = new HashMap<>(DATA_MAP);
		refDictDataMap.put("stringField", "queryReferenceDict");

		var refId = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, refDictDataMap)).getId();

		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put(TESTABLE_DICT_ID, refId);

		dictDataService.create(buildDictDataItem(TESTABLE_REF_DICT_ID, dataMap));

		var query = "integerField = 1 and %s.stringField = 'queryReferenceDict'".formatted(TESTABLE_DICT_ID);

		var actual = dictDataService.getByFilter(TESTABLE_REF_DICT_ID, List.of("*", TESTABLE_DICT_ID + ".stringField"), query, Pageable.unpaged())
				.getContent()
				.stream()
				.map(DictItem::getData)
				.map(Map::entrySet)
				.flatMap(Collection::stream)
				.filter(it -> TESTABLE_DICT_ID.equals(it.getKey()))
				.map(Map.Entry::getValue)
				.map(it -> (DictItem) it)
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Query reference test failed."));

		assertEquals(refId, actual.getId());
		assertEquals(refDictDataMap.get("stringField"), actual.getData().get("stringField"));
	}

	protected void getByFilterWithArrayContainsAnyValue()
	{
		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("timestampField", List.of("2021-08-15T06:00:00.000Z", "2023-04-15T06:00:00.000Z", "2019-08-15T06:00:00.000Z"));

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, dataMap));

		var anyQuery = "timestampField any ['2021-08-15T06:00:00'::timestamp, '2001-08-16T08:00:00'::timestamp, '2006-08-16T08:00:00'::timestamp]";

		var actualIds = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), anyQuery, Pageable.unpaged())
				.getContent()
				.stream()
				.map(DictItem::getId)
				.collect(Collectors.toSet());

		assertTrue(actualIds.contains(dictItem.getId()));
	}

	protected void getByFilterWithArrayContainsAllValue()
	{
		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("timestampField", List.of("2021-08-15T06:00:00.000Z", "2001-08-16T08:00:00.000Z", "2006-08-16T08:00:00.000Z"));

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, dataMap));

		var allQuery = "timestampField all ['2021-08-15T06:00:00'::timestamp, '2001-08-16T08:00:00'::timestamp, '2006-08-16T08:00:00'::timestamp]";

		var actualIds = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), allQuery, Pageable.unpaged())
				.getContent()
				.stream()
				.map(DictItem::getId)
				.collect(Collectors.toSet());

		assertTrue(actualIds.contains(dictItem.getId()));
	}

	protected void existsById()
	{
		var item = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), "integerField = 1", PageRequest.of(0, 1))
				.toList()
				.get(0);

		assertTrue(dictDataService.existsById(TESTABLE_DICT_ID, item.getId()));

		assertFalse(dictDataService.existsById(TESTABLE_DICT_ID, "-1"));
	}

	protected void existsByFilter()
	{
		assertTrue(dictDataService.existsByFilter(TESTABLE_DICT_ID, "integerField = 1"));

		assertFalse(dictDataService.existsByFilter(TESTABLE_DICT_ID, "integerField = 2"));
	}

	protected void attachmentBindingWithCreateDictItem()
	{
		var attachmentDataMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", firstAttachmentId,
				"attachmentsField", List.of(firstAttachmentId),
				"booleanField", true);

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_ATTACH_DICT_ID, attachmentDataMap));

		var singleFieldAttachments = attachmentService.getAttachments(
				DICTS_ATTACHMENT_TYPE_TEMPLATE.formatted(TESTABLE_ATTACH_DICT_ID, dictItem.getId(), "attachmentField"), dictItem.getId());
		var multivaluedFieldAttachments = attachmentService.getAttachments(
				DICTS_ATTACHMENT_TYPE_TEMPLATE.formatted(TESTABLE_ATTACH_DICT_ID, dictItem.getId(), "attachmentsField"), dictItem.getId());

		assertNotNull(dictItem);
		assertEquals(firstAttachment.getId(), singleFieldAttachments.get(0).getId());
		assertEquals(firstAttachment.getId(), multivaluedFieldAttachments.get(0).getId());
	}

	protected void checkAttachmentBindingWithUpdateDictItem()
	{
		var attachmentDataMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentsField", List.of(secondAttachmentId, thirdAttachmentId),
				"booleanField", true);

		var attachmentDataUpdateMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", secondAttachmentId,
				"attachmentsField", List.of(secondAttachmentId, thirdAttachmentId),
				"booleanField", false);

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_ATTACH_DICT_ID, attachmentDataMap));

		var updatedDictItem = dictDataService.update(dictItem.getId(), buildDictDataItem(TESTABLE_ATTACH_DICT_ID, attachmentDataUpdateMap), dictItem.getVersion());

		var singleFieldAttachments = attachmentService.getAttachments(
				DICTS_ATTACHMENT_TYPE_TEMPLATE.formatted(TESTABLE_ATTACH_DICT_ID, updatedDictItem.getId(), "attachmentField"), updatedDictItem.getId());
		var multivaluedFieldAttachments = attachmentService.getAttachments(
				DICTS_ATTACHMENT_TYPE_TEMPLATE.formatted(TESTABLE_ATTACH_DICT_ID, updatedDictItem.getId(), "attachmentsField"), updatedDictItem.getId());

		assertNotNull(dictItem);
		assertEquals(secondAttachment.getId(), singleFieldAttachments.get(0).getId());
		assertEquals(multivaluedFieldAttachments.size(), 2);
		assertEquals(secondAttachment.getId(), multivaluedFieldAttachments.get(0).getId());
		assertEquals(thirdAttachment.getId(), multivaluedFieldAttachments.get(1).getId());
	}

	protected void checkAttachmentReleaseWithDeleteDictItem()
	{
		var attachmentDataMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"booleanField", true,
				"attachmentField", firstAttachmentId,
				"attachmentsField", List.of(firstAttachmentId));

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_ATTACH_DICT_ID, attachmentDataMap));

		dictDataService.delete(TESTABLE_ATTACH_DICT_ID, dictItem.getId(), true, dictItem.getVersion());

		var attachmentsAfterDelete = attachmentService.getAttachments(
				DICTS_ATTACHMENT_TYPE_TEMPLATE.formatted(TESTABLE_ATTACH_DICT_ID, dictItem.getId(), "attachmentField"), dictItem.getId());

		assertTrue(attachmentsAfterDelete.isEmpty());
	}

	protected void checkAttachmentBindingWithDeleteDictItem()
	{
		var attachmentDataMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", firstAttachmentId,
				"attachmentsField", List.of(firstAttachmentId),
				"booleanField", true);

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_ATTACH_DICT_ID, attachmentDataMap));

		dictDataService.delete(TESTABLE_ATTACH_DICT_ID, dictItem.getId(), true, dictItem.getVersion());

		var attachmentsAfterDelete = attachmentService.getAttachments(
				DICTS_ATTACHMENT_TYPE_TEMPLATE.formatted(TESTABLE_ATTACH_DICT_ID, dictItem.getId(), "attachmentField"), dictItem.getId());

		assertTrue(attachmentsAfterDelete.isEmpty());

		var deletedDictItem = dictDataService.getById(TESTABLE_ATTACH_DICT_ID, dictItem.getId());

		dictDataService.delete(TESTABLE_ATTACH_DICT_ID, dictItem.getId(), false, deletedDictItem.getVersion());

		var attachmentsAfterRestore = attachmentService.getAttachments(
				DICTS_ATTACHMENT_TYPE_TEMPLATE.formatted(TESTABLE_ATTACH_DICT_ID, deletedDictItem.getId(), "attachmentField"), deletedDictItem.getId());

		assertEquals(attachmentsAfterRestore.size(), 1);
	}

	protected void createDictItem()
	{
		assertNotNull(dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP)));
	}

	protected void createManyDictItems()
	{
		assertNotNull(dictDataService.createMany(TESTABLE_DICT_ID, List.of(DATA_MAP, DATA_MAP, DATA_MAP)));
	}

	protected void createDictItemWithNullData()
	{
		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("doubleField", null);

		dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, dataMap));

		var actual = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), "doubleField = null", Pageable.unpaged())
				.getContent()
				.stream()
				.map(DictItem::getData)
				.map(Map::entrySet)
				.flatMap(Collection::stream)
				.filter(it -> "doubleField".equals(it.getKey()))
				.collect(StreamCollectors.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));

		actual.values().forEach(Assertions::assertNull);
	}

	protected void createWithDifferentType()
	{
		var longDataMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"booleanField", true);

		var doubleDataMap = Map.of(
				"stringField", "string",
				"integerField", 1.0,
				"doubleField", 2.50,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"booleanField", Boolean.TRUE);

		assertNotNull(dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP)));
		assertNotNull(dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, longDataMap)));
		assertNotNull(dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, doubleDataMap)));
	}

	protected void createWithUUIDDictFieldStartNumericIds()
	{
		var fields = new ArrayList<DictField>();
		var fieldId = RandomStringUtils.randomNumeric(1) + org.apache.commons.lang3.StringUtils.substring(generateRandomUUIDWithoutDashes(), 1);

		var dictFieldStartNumeric = DictField.builder()
				.id(fieldId)
				.name("строка")
				.type(DictFieldType.STRING)
				.required(true)
				.multivalued(false)
				.build();

		fields.add(dictFieldStartNumeric);

		var dict = buildDict(RandomStringUtils.randomNumeric(1) + org.apache.commons.lang3.StringUtils.substring(generateRandomUUIDWithoutDashes(), 1));
		dict.setFields(fields);

		var dictId = dictService.create(dict)
				.getId();

		var dictDataItem = buildDictDataItem(dictId, Map.of(fieldId, "1__numericStringValue"));
		var dictItem = dictDataService.create(dictDataItem);

		assertEquals(dictItem.getData().get(fieldId), dictDataItem.getDataItemMap().get(fieldId));

		var updatedDictDataItem = buildDictDataItem(dictId, Map.of(fieldId, "stringValue"));
		var updatedDictItem = dictDataService.update(dictItem.getId(), updatedDictDataItem, dictItem.getVersion());

		assertEquals(updatedDictItem.getData().get(fieldId), updatedDictDataItem.getDataItemMap().get(fieldId));
	}

	protected void createCorrectContainsFieldsInHistoryMap()
	{
		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put(ServiceFieldConstants.ID, "with_id");

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, dataMap));

		var createdFieldResult = dictItem.getHistory().stream().findFirst().map(it -> it.get(ServiceFieldConstants.CREATED)).isPresent();
		var idFieldResult = dictItem.getHistory().stream().findFirst().map(it -> it.get(ServiceFieldConstants.ID)).isPresent();

		assertTrue(createdFieldResult);
		assertFalse(idFieldResult);
	}

	//TODO: расширить тест когда Json указывается строкой
	protected void createDictItemWithJson()
	{
		var dataMap = new HashMap<>(DATA_MAP);

		dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("jsonField", Map.of("lang", "Kotlin", "version", 1.8, "design", "Event Sourcing"));
		dataMap.put("jsonMultivaluedField", List.of(
				Map.of("lang", ".Net", "version", 4, "design", "Ambassador"),
				Map.of("lang", ".Net", "version", 4.8, "design", "Circuit Breaker")
		));

		var actual = dictDataService.create(buildDictDataItem(TESTABLE_JSON_DICT_ID, dataMap));

		assertEquals(3, ((Map<String, Object>) actual.getData().get("jsonField")).size());
		assertEquals(2, ((List<Map<String, Object>>) actual.getData().get("jsonMultivaluedField")).size());
	}

	@SneakyThrows
	protected void createDictItemWithGeoJsonObject()
	{
		var geo = objectMapper.readValue("{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Point\",\"coordinates\":[37.412284,55.603515]}},{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[37.413423,55.604283],[37.41255,55.60361],[37.413995,55.602974],[37.414842,55.603629],[37.413423,55.604283]]]}},{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[37.411999,55.603159],[37.413568,55.602384],[37.41522,55.603516]]}}]}",
				GeoJsonObject.class);
		var geoJson = objectMapper.writeValueAsString(geo);

		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("geoJsonField", geoJson);
		dataMap.put("geoJsonMultivaluedField", List.of(geoJson, geoJson));
		dataMap.put("stringField", "geoJsonTest");

		dictDataService.create(buildDictDataItem(TESTABLE_GEO_JSON_DICT_ID, dataMap));

		var actual = dictDataService.getByFilter(TESTABLE_GEO_JSON_DICT_ID, List.of("*"), "stringField = 'geoJsonTest'", Pageable.unpaged())
				.getContent();

		assertEquals(FeatureCollection.class, actual.get(0).getData().get("geoJsonField").getClass());
		assertEquals(2, ((List<FeatureCollection>) actual.get(0).getData().get("geoJsonMultivaluedField")).size());
	}

	protected void updateDictItem()
	{
		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP));

		var timestampFields = new ArrayList<>((List<Object>) DATA_MAP.get("timestampField"));
		timestampFields.add("2021-08-15T11:00:00.000Z");

		var updatedDataMap = new HashMap<>(DATA_MAP);
		updatedDataMap.put("integerField", 3);
		updatedDataMap.put("timestampField", timestampFields);
		updatedDataMap.put("booleanField", false);

		var actual = dictDataService.update(dictItem.getId(), buildDictDataItem(TESTABLE_DICT_ID, updatedDataMap), dictItem.getVersion());

		assertEquals(dictItem.getVersion() + 1, actual.getVersion());

		assertEquals(3, ((List<Object>) actual.getData().get("timestampField")).size());
	}

	protected void updateDictItemWithEmptyMultivaluedData()
	{
		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP));

		var updatedDataMap = new HashMap<>(DATA_MAP);
		updatedDataMap.put("timestampField", null);

		var actual = dictDataService.update(dictItem.getId(), buildDictDataItem(TESTABLE_DICT_ID, updatedDataMap), dictItem.getVersion());

		assertEquals(0, ((List<Object>) actual.getData().get("timestampField")).size());

		dictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP));

		updatedDataMap = new HashMap<>(DATA_MAP);
		updatedDataMap.put("timestampField", List.of());

		actual = dictDataService.update(dictItem.getId(), buildDictDataItem(TESTABLE_DICT_ID, updatedDataMap), dictItem.getVersion());

		assertEquals(0, ((List<Object>) actual.getData().get("timestampField")).size());
	}

	protected void updateDictItemWithJson()
	{
		var dataMap = new HashMap<>(DATA_MAP);

		dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("jsonField", Map.of("lang", "Kotlin", "version", 1.8, "design", "Event Sourcing"));
		dataMap.put("jsonMultivaluedField", List.of(
				Map.of("lang", ".Net", "version", 4, "design", "Ambassador"),
				Map.of("lang", ".Net", "version", 4.8, "design", "Circuit Breaker")
		));

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_JSON_DICT_ID, dataMap));

		var updatableMap = new HashMap<>(dictItem.getData());
		updatableMap.put("jsonField", Map.of("lang", "Kotlin", "version", 1.7, "design", "Event Sourcing"));
		updatableMap.put("jsonMultivaluedField", List.of(
				Map.of("lang", ".Net", "version", 4.1, "design", "Ambassador"),
				Map.of("lang", "Java", "version", 8, "design", "2PC")
		));

		dictDataService.update(dictItem.getId(), buildDictDataItem(TESTABLE_JSON_DICT_ID, updatableMap), dictItem.getVersion());

		var actual = dictDataService.getById(TESTABLE_JSON_DICT_ID, dictItem.getId());

		assertEquals(updatableMap, actual.getData());
	}

	@SneakyThrows
	protected void updateDictItemWithGeoJson()
	{
		var geo = objectMapper.readValue("{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[55.68154659727316,37.553090921089115],[55.646161942996564,37.58087155165372]]]}}", GeoJsonObject.class);
		var geoJson = objectMapper.writeValueAsString(geo);

		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("geoJsonField", geoJson);
		dataMap.put("geoJsonMultivaluedField", List.of(geoJson, geoJson));

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_GEO_JSON_DICT_ID, dataMap));

		var updatableMap = new HashMap<>(dictItem.getData());

		var updatableGeo = objectMapper.readValue("{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[55.68154659711111,37.553090921011111],[55.646161144006644,37.58087155668877]]]}}", GeoJsonObject.class);
		var updatableGeoJson = objectMapper.writeValueAsString(updatableGeo);

		updatableMap.put("geoJsonField", updatableGeoJson);
		updatableMap.put("geoJsonMultivaluedField", List.of(updatableGeoJson));

		dictDataService.update(dictItem.getId(), buildDictDataItem(TESTABLE_GEO_JSON_DICT_ID, updatableMap), dictItem.getVersion());

		var actual = dictDataService.getById(TESTABLE_GEO_JSON_DICT_ID, dictItem.getId());

		assertEquals(Feature.class, actual.getData().get("geoJsonField").getClass());
		assertEquals(updatableGeo, actual.getData().get("geoJsonField"));
		assertEquals(1, ((List<GeoJsonObject>) actual.getData().get("geoJsonMultivaluedField")).size());
	}

	protected void updateConcurrentExc()
	{
		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP));

		var updatedDataMap = new HashMap<>(DATA_MAP);
		updatedDataMap.put("integerField", 3);

		dictDataService.update(dictItem.getId(), buildDictDataItem(TESTABLE_DICT_ID, updatedDataMap), dictItem.getVersion());

		assertThrows(DictConcurrentUpdateException.class,
				() -> dictDataService.update(dictItem.getId(), buildDictDataItem(TESTABLE_DICT_ID, updatedDataMap), dictItem.getVersion()));
	}

	protected void deleteDictItem()
	{
		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP));

		dictDataService.delete(TESTABLE_DICT_ID, dictItem.getId(), true, dictItem.getVersion());
		assertNotNull(dictDataService.getById(TESTABLE_DICT_ID, dictItem.getId()).getDeleted());

		var deletedDictItem = dictDataService.getById(TESTABLE_DICT_ID, dictItem.getId());

		dictDataService.delete(TESTABLE_DICT_ID, dictItem.getId(), false, deletedDictItem.getVersion());
		assertNull(dictDataService.getById(TESTABLE_DICT_ID, dictItem.getId()).getDeleted());

	}

	protected void deleteWithReason(String reason)
	{
		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP));

		dictDataService.delete(TESTABLE_DICT_ID, dictItem.getId(), true, reason, dictItem.getVersion());

		var result = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), "deleted != null", Pageable.unpaged())
				.stream()
				.filter(item -> Objects.equals(item.getId(), dictItem.getId()))
				.findFirst()
				.orElseThrow(() -> new ObjectNotFoundException(DictItem.class, dictItem.getId()));

		assertEquals(reason, result.getDeletionReason());
	}

	protected void deleteWithEmptyReason()
	{
		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP));

		var reason = "";
		dictDataService.delete(TESTABLE_DICT_ID, dictItem.getId(), true, reason, dictItem.getVersion());

		var result = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), "deleted != null", Pageable.unpaged())
				.stream()
				.filter(item -> Objects.equals(item.getId(), dictItem.getId()))
				.findFirst()
				.orElseThrow(() -> new ObjectNotFoundException(DictItem.class, dictItem.getId()));

		assertNull(result.getDeletionReason());
	}

	private Attachment createAttachment(Resource fileResource) throws IOException
	{
		var bytes = IOUtils.toByteArray(fileResource.getInputStream());
		var attachment = attachmentService.addAttachment(Objects.requireNonNull(fileResource.getFilename()), MediaType.IMAGE_PNG_VALUE, UserInfo.SYSTEM_USER_ID, bytes);
		var savedData = attachmentService.getAttachmentData(attachment.getId());

		assertEquals(attachment.getSize(), (Integer) bytes.length);
		assertArrayEquals(bytes, IOUtils.toByteArray(savedData.getInputStream()));

		return attachment;
	}

	private String createDictHierarchy()
	{
		final var stringField = "stringField";
		final var integerField = "integerField";
		final var doubleField = "doubleField";
		final var timestampField = "timestampField";
		final var booleanField = "booleanField";

		Supplier<Map<String, Object>> testDataMapFactory = () -> ImmutableMap.of(
				stringField, RandomStringUtils.randomAlphabetic(10),
				integerField, RandomUtils.nextInt(0, 128),
				doubleField, RandomUtils.nextDouble(0.0, 128.0),
				timestampField, DateConstants.ISO_OFFSET_DATE_TIME_MS_FORMATTER.format(ZonedDateTime.now()),
				booleanField, RandomUtils.nextBoolean());

		Function<String, Map<String, Object>> testRefDataMapFactory = (String id) ->
				ImmutableMap.<String, Object>builder()
						.putAll(testDataMapFactory.get())
						.put(TESTABLE_DICT_ID, id)
						.build();

		IntStream.range(0, 5)
				.boxed()
				.map(i -> testDataMapFactory.get())
				.map(dataMap -> dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, dataMap)).getId())
				.map(testRefDataMapFactory)
				.forEach(dataMap -> dictDataService.create(buildDictDataItem(TESTABLE_REF_DICT_ID, dataMap)));

		return integerField;
	}

	protected DictDataItem buildDictDataItem(String dictId, Map<String, Object> dataMap)
	{
		return DictDataItem.of(dictId, dataMap);
	}
}
