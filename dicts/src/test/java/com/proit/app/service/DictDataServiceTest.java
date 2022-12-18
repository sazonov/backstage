package com.proit.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableMap;
import com.proit.app.common.AbstractTest;
import com.proit.app.domain.DictItem;
import com.proit.app.exception.DictionaryConcurrentUpdateException;
import com.proit.app.exception.ObjectNotFoundException;
import com.proit.app.model.domain.Attachment;
import com.proit.app.model.other.user.UserInfo;
import com.proit.app.service.attachment.AttachmentService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.ThrowingConsumer;
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


	public static final Map<String, Object> DOC = Map.of(
			"stringField", "string",
			"integerField", 1,
			"doubleField", 2,
			"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
			"booleanField", true);

	@Autowired
	private DictDataService dictDataService;

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
				"integerField = 1 or stringField like 'str' or integerField in (2, 5, 8) and integerField != 10 or integerField <= 2",
				PageRequest.of(0, 10));

		assertEquals(result.getContent().get(0).getData().get("stringField"), DOC.get("stringField"));
	}

	@Test
	void getByFilterWithRefCorrect()
	{
		var refId = dictDataService.create(DICT_ID, DOC).getId();

		var doc = new HashMap<>(DOC);
		doc.put(DICT_ID, refId);

		dictDataService.create(REF_DICT_ID, doc);

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
		var attachmentFieldDoc = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", firstAttachmentId,
				"booleanField", true);

		var dictItem = dictDataService.create(DICT_ATTACHMENT_ID, attachmentFieldDoc);
		var attachments = attachmentService.getAttachments(
				DICTS_ATTACHMENT_TYPE.formatted(DICT_ATTACHMENT_ID, dictItem.getId(), "attachmentField"), dictItem.getId());

		assertNotNull(dictItem);
		assertEquals(firstAttachment.getId(), attachments.get(0).getId());
	}

	@Test
	void checkAttachmentBindingIfUpdateCorrect()
	{
		var attachmentFieldDoc = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", firstAttachmentId,
				"booleanField", true);

		var attachmentFieldDocUpdate = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", secondAttachmentId,
				"booleanField", false);

		var dictItem = dictDataService.create(DICT_ATTACHMENT_ID, attachmentFieldDoc);

		var updatedDictItem = dictDataService.update(DICT_ATTACHMENT_ID, dictItem.getId(), dictItem.getVersion(), attachmentFieldDocUpdate);

		var attachments = attachmentService.getAttachments(
				DICTS_ATTACHMENT_TYPE.formatted(DICT_ATTACHMENT_ID, updatedDictItem.getId(), "attachmentField"), updatedDictItem.getId());

		assertNotNull(dictItem);
		assertEquals(secondAttachment.getId(), attachments.get(0).getId());
	}

	@Test
	void checkAttachmentReleaseIfDeleteCorrect()
	{
		var attachmentFieldDoc = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"booleanField", true,
				"attachmentField", firstAttachmentId);

		var dictItem = dictDataService.create(DICT_ATTACHMENT_ID, attachmentFieldDoc);

		dictDataService.delete(DICT_ATTACHMENT_ID, dictItem.getId(), true);

		assertTrue(attachmentService.getAttachment(firstAttachmentId).getBindings().isEmpty());
	}

	@Test
	void checkAttachmentBindingIfDeleteCorrect()
	{
		var attachmentFieldDoc = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", firstAttachmentId,
				"booleanField", true);
		var dictItem = dictDataService.create(DICT_ATTACHMENT_ID, attachmentFieldDoc);

		dictDataService.delete(DICT_ATTACHMENT_ID, dictItem.getId(), true);
		assertTrue(attachmentService.getAttachment(firstAttachmentId).getBindings().isEmpty());

		dictDataService.delete(DICT_ATTACHMENT_ID, dictItem.getId(), false);
		assertFalse(attachmentService.getAttachment(firstAttachmentId).getBindings().isEmpty());
	}


	@Test
	void createCorrect()
	{
		assertNotNull(dictDataService.create(DICT_ID, DOC));
	}

	@Test
	void createWithDifferentTypeCorrect()
	{
		var longFieldDoc = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"booleanField", true);

		var doubleFieldDoc = Map.of(
				"stringField", "string",
				"integerField", 1.0,
				"doubleField", 2.50,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"booleanField", Boolean.TRUE);

		assertNotNull(dictDataService.create(DICT_ID, DOC));
		assertNotNull(dictDataService.create(DICT_ID, longFieldDoc));
		assertNotNull(dictDataService.create(DICT_ID, doubleFieldDoc));
	}

	@Test
	void updateCorrect()
	{
		var dictItem = dictDataService.create(DICT_ID, DOC);

		var updatedDoc = new HashMap<>(DOC);
		updatedDoc.put("integerField", 3);
		updatedDoc.put("timestampField", null);
		updatedDoc.put("booleanField", false);

		assertNotNull(dictDataService.update(DICT_ID, dictItem.getId(), dictItem.getVersion(), updatedDoc));
	}

	@Test
	void updateConcurrentExc()
	{
		var dictItem = dictDataService.create(DICT_ID, DOC);

		var updatedDoc = new HashMap<>(DOC);
		updatedDoc.put("integerField", 3);

		dictDataService.update(DICT_ID, dictItem.getId(), dictItem.getVersion(), updatedDoc);

		assertThrows(DictionaryConcurrentUpdateException.class,
				() -> dictDataService.update(DICT_ID, dictItem.getId(), dictItem.getVersion(), updatedDoc));
	}

	@Test
	void deleteCorrect()
	{
		var dictItem = dictDataService.create(DICT_ID, DOC);

		dictDataService.delete(DICT_ID, dictItem.getId(), true);
		dictDataService.delete(DICT_ID, dictItem.getId(), false);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"Test reason", "1234"})
	void delete_withReason(String reason)
	{
		var dictItem = dictDataService.create(DICT_ID, DOC);

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
		var dictItem = dictDataService.create(DICT_ID, DOC);

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
		var dictItems = List.of(DOC, DOC);

		assertNotNull(dictDataService.createMany(DICT_ID, dictItems));
	}

	@Test
	void getByIdsCorrect()
	{
		var ids = dictDataService.createMany(DICT_ID, List.of(DOC, DOC, DOC, DOC))
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
	void getByFilterWithDifferentDateCorrect()
	{
		var now = Date.from(LocalDateTime.of(2021, 8, 15, 6, 0, 0).atZone(ZoneId.systemDefault()).toInstant());

		Map<String, Object> stringDateDoc = Map.of(
				"stringField", "string",
				"integerField", 1.0,
				"doubleField", 2.50,
				"timestampField", "2021-08-15T06:00:00.000Z",
				"booleanField", Boolean.TRUE);

		Map<String, Object> objectDateDoc = Map.of(
				"stringField", "string",
				"integerField", 1.0,
				"doubleField", 2.50,
				"timestampField", now,
				"booleanField", Boolean.FALSE);

		var stringDateItemId = dictDataService.create(DICT_ID, stringDateDoc).getId();
		var objectDateItemId = dictDataService.create(DICT_ID, objectDateDoc).getId();

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

		Supplier<Map<String, Object>> testDocFactory = () -> ImmutableMap.of(
				stringField, RandomStringUtils.randomAlphabetic(10),
				integerField, RandomUtils.nextInt(0, 128),
				doubleField, RandomUtils.nextDouble(0.0, 128.0),
				timestampField, MappingService.DATE_TIME_FORMATTER.format(ZonedDateTime.now()),
				booleanField, RandomUtils.nextBoolean());

		Function<String, Map<String, Object>> testRefDocFactory = (String id) ->
				ImmutableMap.<String, Object>builder()
						.putAll(testDocFactory.get())
						.put(DICT_ID, id)
						.build();


		IntStream.range(0, 5)
				.boxed()
				.map(i -> testDocFactory.get())
				.map(doc -> dictDataService.create(DICT_ID, doc).getId())
				.map(testRefDocFactory)
				.forEach(doc -> dictDataService.create(REF_DICT_ID, doc));

		return integerField;
	}
}