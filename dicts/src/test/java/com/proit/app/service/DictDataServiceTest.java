package com.proit.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableMap;
import com.proit.app.common.AbstractTest;
import com.proit.app.constant.ServiceFieldConstants;
import com.proit.app.domain.DictItem;
import com.proit.app.exception.ObjectNotFoundException;
import com.proit.app.exception.dictionary.DictConcurrentUpdateException;
import com.proit.app.model.dictitem.DictDataItem;
import com.proit.app.model.domain.Attachment;
import com.proit.app.model.other.date.DateConstants;
import com.proit.app.model.other.user.UserInfo;
import com.proit.app.service.attachment.AttachmentService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.stream.IntStream;

import static com.proit.app.service.advice.BindingDictDataServiceAdvice.DICTS_ATTACHMENT_TYPE;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DictDataServiceTest extends AbstractTest
{
	@Autowired
	private AttachmentService attachmentService;

	@Autowired
	private ObjectMapper mapper;

	@Value("classpath:attachment.png")
	private Resource firstFileResource;

	@Value("classpath:attachment2.png")
	private Resource secondFileResource;

	private String firstAttachmentId;
	private String secondAttachmentId;

	private Attachment firstAttachment;
	private Attachment secondAttachment;

	public static final Map<String, Object> DATA_MAP = Map.of(
			"stringField", "string",
			"integerField", 1,
			"doubleField", 2,
			"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
			"booleanField", true);

	@BeforeAll
	public void setUpAttachment() throws IOException
	{
		firstAttachment = createAttachment(firstFileResource);
		secondAttachment = createAttachment(secondFileResource);

		firstAttachmentId = firstAttachment.getId();
		secondAttachmentId = secondAttachment.getId();
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

	@Test
	void getByFilterCorrect()
	{
		var result = dictDataService.getByFilter(DICT_ID, List.of("*"),
				"integerField = 1 or stringField like 'str' or integerField in (2, 5, 8) and integerField != 10 or integerField <= 2 and doubleField > 1.9 and doubleField < 2.1",
				PageRequest.of(0, 10));

		assertEquals(result.getContent().get(0).getData().get("stringField"), DATA_MAP.get("stringField"));
	}

	@Test
	void getIdsByFilterCorrect()
	{
		var result = dictDataService.getIdsByFilter(DICT_ID, "integerField = 1 or stringField like 'str' or integerField in (2, 5, 8) and integerField != 10 or integerField <= 2 and doubleField > 1.9 and doubleField < 2.1");

		assertNotNull(result.getContent().get(0));
	}

	@Test
	void getByFilterWithRefCorrect()
	{
		var refId = dictDataService.create(buildDictDataItem(DICT_ID, DATA_MAP)).getId();

		var refDataMap = new HashMap<>(DATA_MAP);
		refDataMap.put(DICT_ID, refId);

		dictDataService.create(buildDictDataItem(REF_DICT_ID, refDataMap));

		var result = dictDataService.getByFilter(REF_DICT_ID, List.of("*", DICT_ID + ".timestampField"), null, PageRequest.of(0, 10));

		assertNotNull(result.getContent().get(0).getData().get(DICT_ID));
	}

	@Test
	void existsByIdCorrect()
	{
		var item = dictDataService.getByFilter(DICT_ID, List.of("*"), "integerField = 1", PageRequest.of(0, 1))
				.toList()
				.get(0);

		assertTrue(dictDataService.existsById(DICT_ID, item.getId()));

		assertFalse(dictDataService.existsById(DICT_ID, "-1"));
	}

	@Test
	void existsByFilterCorrect()
	{
		assertTrue(dictDataService.existsByFilter(DICT_ID, "integerField = 1"));

		assertFalse(dictDataService.existsByFilter(DICT_ID, "integerField = 2"));
	}

	@Test
	void checkAttachmentBindingIfCreateCorrect()
	{
		var attachmentDataMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", firstAttachmentId,
				"booleanField", true);

		var dictItem = dictDataService.create(buildDictDataItem(DICT_ATTACHMENT_ID, attachmentDataMap));
		var attachments = attachmentService.getAttachments(
				DICTS_ATTACHMENT_TYPE.formatted(DICT_ATTACHMENT_ID, dictItem.getId(), "attachmentField"), dictItem.getId());

		assertNotNull(dictItem);
		assertEquals(firstAttachment.getId(), attachments.get(0).getId());
	}

	@Test
	void checkAttachmentBindingIfUpdateCorrect()
	{
		var attachmentDataMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", firstAttachmentId,
				"booleanField", true);

		var attachmentDataUpdateMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", secondAttachmentId,
				"booleanField", false);

		var dictItem = dictDataService.create(buildDictDataItem(DICT_ATTACHMENT_ID, attachmentDataMap));

		var updatedDictItem = dictDataService.update(dictItem.getId(), buildDictDataItem(DICT_ATTACHMENT_ID, attachmentDataUpdateMap), dictItem.getVersion());

		var attachments = attachmentService.getAttachments(
				DICTS_ATTACHMENT_TYPE.formatted(DICT_ATTACHMENT_ID, updatedDictItem.getId(), "attachmentField"), updatedDictItem.getId());

		assertNotNull(dictItem);
		assertEquals(secondAttachment.getId(), attachments.get(0).getId());
	}

	@Test
	void checkAttachmentReleaseIfDeleteCorrect()
	{
		var attachmentDataMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"booleanField", true,
				"attachmentField", firstAttachmentId);

		var dictItem = dictDataService.create(buildDictDataItem(DICT_ATTACHMENT_ID, attachmentDataMap));

		dictDataService.delete(DICT_ATTACHMENT_ID, dictItem.getId(), true);

		assertTrue(attachmentService.getAttachment(firstAttachmentId).getBindings().isEmpty());
	}

	@Test
	void checkAttachmentBindingIfDeleteCorrect()
	{
		var attachmentDataMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", firstAttachmentId,
				"booleanField", true);
		var dictItem = dictDataService.create(buildDictDataItem(DICT_ATTACHMENT_ID, attachmentDataMap));

		dictDataService.delete(DICT_ATTACHMENT_ID, dictItem.getId(), true);
		assertTrue(attachmentService.getAttachment(firstAttachmentId).getBindings().isEmpty());

		dictDataService.delete(DICT_ATTACHMENT_ID, dictItem.getId(), false);
		assertFalse(attachmentService.getAttachment(firstAttachmentId).getBindings().isEmpty());
	}

	@Test
	void createCorrect()
	{
		assertNotNull(dictDataService.create(buildDictDataItem(DICT_ID, DATA_MAP)));
	}

	@Test
	void createWithDifferentTypeCorrect()
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

		assertNotNull(dictDataService.create(buildDictDataItem(DICT_ID, DATA_MAP)));
		assertNotNull(dictDataService.create(buildDictDataItem(DICT_ID, longDataMap)));
		assertNotNull(dictDataService.create(buildDictDataItem(DICT_ID, doubleDataMap)));
	}

	@Test
	void createHistoryNotContainIdField()
	{
		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put(ServiceFieldConstants.ID, "with_id");

		var dictItem = dictDataService.create(buildDictDataItem(DICT_ID, dataMap));

		var createdFieldResult = dictItem.getHistory().stream().findFirst().map(it -> it.get(ServiceFieldConstants.CREATED)).isPresent();
		var idFieldResult = dictItem.getHistory().stream().findFirst().map(it -> it.get(ServiceFieldConstants.ID)).isPresent();

		assertTrue(createdFieldResult);
		assertFalse(idFieldResult);
	}

	@Test
	void updateCorrect()
	{
		var dictItem = dictDataService.create(buildDictDataItem(DICT_ID, DATA_MAP));

		var updatedDataMap = new HashMap<>(DATA_MAP);
		updatedDataMap.put("integerField", 3);
		updatedDataMap.put("timestampField", null);
		updatedDataMap.put("booleanField", false);

		assertNotNull(dictDataService.update(dictItem.getId(), buildDictDataItem(DICT_ID, updatedDataMap), dictItem.getVersion()));
	}

	@Test
	void updateConcurrentExc()
	{
		var dictItem = dictDataService.create(buildDictDataItem(DICT_ID, DATA_MAP));

		var updatedDataMap = new HashMap<>(DATA_MAP);
		updatedDataMap.put("integerField", 3);

		dictDataService.update(dictItem.getId(), buildDictDataItem(DICT_ID, updatedDataMap), dictItem.getVersion());

		assertThrows(DictConcurrentUpdateException.class,
				() -> dictDataService.update(dictItem.getId(), buildDictDataItem(DICT_ID, updatedDataMap), dictItem.getVersion()));
	}

	@Test
	void deleteCorrect()
	{
		var dictItem = dictDataService.create(buildDictDataItem(DICT_ID, DATA_MAP));

		dictDataService.delete(DICT_ID, dictItem.getId(), true);
		dictDataService.delete(DICT_ID, dictItem.getId(), false);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"Test reason", "1234"})
	void delete_withReason(String reason)
	{
		var dictItem = dictDataService.create(buildDictDataItem(DICT_ID, DATA_MAP));

		dictDataService.delete(DICT_ID, dictItem.getId(), true, reason);

		var result = dictDataService.getByFilter(DICT_ID, List.of(), "deleted != null", Pageable.unpaged())
				.stream()
				.filter(item -> Objects.equals(item.getId(), dictItem.getId()))
				.findFirst()
				.orElseThrow(() -> new ObjectNotFoundException(DictItem.class, dictItem.getId()));

		assertEquals(reason, result.getDeletionReason());
	}

	@Test
	void delete_withEmptyReason()
	{
		var dictItem = dictDataService.create(buildDictDataItem(DICT_ID, DATA_MAP));

		var reason = "";
		dictDataService.delete(DICT_ID, dictItem.getId(), true, reason);

		var result = dictDataService.getByFilter(DICT_ID, List.of(), "deleted != null", Pageable.unpaged())
				.stream()
				.filter(item -> Objects.equals(item.getId(), dictItem.getId()))
				.findFirst()
				.orElseThrow(() -> new ObjectNotFoundException(DictItem.class, dictItem.getId()));

		assertNull(result.getDeletionReason());
	}

	@Test
	void createMany()
	{
		assertNotNull(dictDataService.createMany(DICT_ID, List.of(DATA_MAP, DATA_MAP)));
	}

	@Test
	void getByIdsCorrect()
	{
		var ids = dictDataService.createMany(DICT_ID, List.of(DATA_MAP, DATA_MAP, DATA_MAP, DATA_MAP))
				.stream()
				.map(DictItem::getId)
				.toList();

		assertArrayEquals(ids.toArray(String[]::new), dictDataService.getByIds(DICT_ID, ids).stream().map(DictItem::getId).toArray(String[]::new));
	}

	private final ThrowingConsumer<Object> objectWriter = obj -> System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj));

	@Test
	void getByFilter_innerDictSort()
	{
		final String integerField = createDictHierarchy();

		var result = dictDataService.getByFilter(REF_DICT_ID, List.of("*", DICT_ID + ".*"), null,
						PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, DICT_ID + "." + integerField)))
				.getContent()
				.stream()
				.map(DictItem::getData)
				.toList();

		objectWriter.accept(result);

		boolean actual = Comparators.isInOrder(result, Comparator.comparing((Map<String, Object> data) -> (Long) ((DictItem) data.get(DICT_ID)).getData().get(integerField)).reversed());

		assertTrue(actual);
	}

	@Test
	void getByFilter_innerDictSortWrongFiledName()
	{
		var orderField = createDictHierarchy();

		var result = dictDataService.getByFilter(REF_DICT_ID, List.of("*", DICT_ID + ".*"), null,
						PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, DICT_ID + "." + "wrongFiled")))
				.getContent()
				.stream()
				.map(DictItem::getData)
				.toList();

		objectWriter.accept(result);

		boolean actual = Comparators.isInOrder(result, Comparator.comparing((Map<String, Object> data) -> (Long) ((DictItem) data.get(DICT_ID)).getData().get(orderField)).reversed());

		assertFalse(actual);
	}

	@Test
	void getByFilter_dictSortServiceField()
	{
		createDictHierarchy();

		var sortedIdFields = dictDataService.getByFilter(DICT_ID, List.of("*"), null,
						PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, ServiceFieldConstants.ID)))
				.getContent()
				.stream()
				.map(DictItem::getId)
				.toList();

		var sortedCreatedFields = dictDataService.getByFilter(DICT_ID, List.of("*"), null,
						PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, ServiceFieldConstants.CREATED)))
				.getContent()
				.stream()
				.map(DictItem::getCreated)
				.toList();

		var actualIdFields = Comparators.isInOrder(sortedIdFields, Comparator.reverseOrder());
		var actualCreatedFields = Comparators.isInOrder(sortedCreatedFields, Comparator.naturalOrder());

		assertTrue(actualIdFields);
		assertTrue(actualCreatedFields);
	}

	@Test
	void getByFilter_dictSortDataField()
	{
		var sortedDataField = createDictHierarchy();

		var result = dictDataService.getByFilter(DICT_ID, List.of("*"), null,
						PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, sortedDataField)))
				.getContent()
				.stream()
				.map(DictItem::getData)
				.map(map -> (Long) map.get(sortedDataField))
				.toList();

		var actual = Comparators.isInOrder(result, Comparator.reverseOrder());

		assertTrue(actual);
	}

	//TODO: При реализации валидации переданных клиентом сервисных для адаптера полей, актуализировать тест.
	@Test
	void getByFilter_withServiceSelectField()
	{
		createDictHierarchy();

		var result = dictDataService.getByFilter(REF_DICT_ID, List.of(ServiceFieldConstants._ID, ServiceFieldConstants.CREATED),
				null, PageRequest.of(0, 20))
				.getContent()
				.stream()
				.map(it -> StringUtils.hasText(it.getId()) && it.getCreated() != null)
				.toList();

		result.forEach(Assertions::assertTrue);
	}

	@Test
	void getByFilterWithDifferentDateCorrect()
	{
		var now = Date.from(LocalDateTime.of(2021, 8, 15, 6, 0, 0).atZone(ZoneId.systemDefault()).toInstant());

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
				"timestampField", now,
				"booleanField", Boolean.FALSE);

		var stringDateItemId = dictDataService.create(buildDictDataItem(DICT_ID, stringDateDataMap)).getId();
		var objectDateItemId = dictDataService.create(buildDictDataItem(DICT_ID, objectDateDataMap)).getId();

		var stringDateItem = dictDataService.getById(DICT_ID, stringDateItemId);
		var objectDateItem = dictDataService.getById(DICT_ID, objectDateItemId);

		assertEquals(stringDateItem.getData().get("timestampField"), List.of(now));
		assertEquals(objectDateItem.getData().get("timestampField"), List.of(now));
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
				timestampField, DateConstants.DATE_TIME_FORMATTER.format(ZonedDateTime.now()),
				booleanField, RandomUtils.nextBoolean());

		Function<String, Map<String, Object>> testRefDataMapFactory = (String id) ->
				ImmutableMap.<String, Object>builder()
						.putAll(testDataMapFactory.get())
						.put(DICT_ID, id)
						.build();

		IntStream.range(0, 5)
				.boxed()
				.map(i -> testDataMapFactory.get())
				.map(dataMap -> dictDataService.create(buildDictDataItem(DICT_ID, dataMap)).getId())
				.map(testRefDataMapFactory)
				.forEach(dataMap -> dictDataService.create(buildDictDataItem(REF_DICT_ID, dataMap)));

		return integerField;
	}

	private DictDataItem buildDictDataItem(String dictId, Map<String, Object> dataMap)
	{
		return DictDataItem.of(dictId, dataMap);
	}
}