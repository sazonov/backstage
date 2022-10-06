package com.proit.app.service;

import com.proit.app.common.AbstractTest;
import com.proit.app.domain.DictItem;
import com.proit.app.exception.DictionaryConcurrentUpdateException;
import com.proit.app.model.domain.Attachment;
import com.proit.app.model.other.user.UserInfo;
import com.proit.app.service.attachment.AttachmentService;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.proit.app.service.advice.BindingDictDataServiceAdvice.DICTS_ATTACHMENT_TYPE;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DictDataServiceTest extends AbstractTest
{
	@Autowired
	private AttachmentService attachmentService;

	@Value("classpath:attachment.png")
	private Resource firstFileResource;

	@Value("classpath:attachment2.png")
	private Resource secondFileResource;

	private String firstAttachmentId;
	private String secondAttachmentId;

	private Attachment firstAttachment;
	private Attachment secondAttaachment;

	public static final Map<String, Object> DOC = Map.of(
			"stringField", "string",
			"integerField", 1,
			"doubleField", 2,
			"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"));

	@Autowired
	private DictDataService dictDataService;

	@BeforeAll
	public void setUpAttachment() throws IOException {
		firstAttachment = createAttachment(firstFileResource);
		secondAttaachment = createAttachment(secondFileResource);

		firstAttachmentId = firstAttachment.getId();
		secondAttachmentId = secondAttaachment.getId();
	}

	private Attachment createAttachment(Resource fileResource) throws IOException {
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
				"attachmentField", firstAttachmentId);
		var dictItem = dictDataService.create(DICT_ATTACHMENT_ID, attachmentFieldDoc);
		var attachments = attachmentService.getAttachments(
			DICTS_ATTACHMENT_TYPE.formatted(DICT_ATTACHMENT_ID, dictItem.getId(), "attachmentField"), dictItem.getId());

		assertNotNull(dictItem);
		assertEquals(firstAttachment.getId() , attachments.get(0).getId());
	}

	@Test
	void checkAttachmentBindingIfUpdateCorrect()
	{
		var attachmentFieldDoc = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", firstAttachmentId);

		var attachmentFieldDocUpdate = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", secondAttachmentId);

		var dictItem = dictDataService.create(DICT_ATTACHMENT_ID, attachmentFieldDoc);

		var updatedDictItem = dictDataService.update(DICT_ATTACHMENT_ID, dictItem.getId(), dictItem.getVersion(), attachmentFieldDocUpdate);

		var attachments = attachmentService.getAttachments(
				DICTS_ATTACHMENT_TYPE.formatted(DICT_ATTACHMENT_ID, updatedDictItem.getId(), "attachmentField"), updatedDictItem.getId());

		assertNotNull(dictItem);
		assertEquals(secondAttaachment.getId() , attachments.get(0).getId());
	}

	@Test
	void checkAttachmentReleaseIfDeleteCorrect()
	{
		var attachmentFieldDoc = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
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
				"attachmentField", firstAttachmentId);
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
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"));

		var doubleFieldDoc = Map.of(
				"stringField", "string",
				"integerField", 1.0,
				"doubleField", 2.50,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"));

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
}