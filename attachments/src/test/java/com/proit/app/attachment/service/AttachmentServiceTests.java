package com.proit.app.attachment.service;

import com.proit.app.attachment.AbstractTests;
import com.proit.app.attachment.model.domain.Attachment;
import com.proit.app.exception.AppException;
import com.proit.app.model.other.user.UserInfo;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AttachmentServiceTests extends AbstractTests
{
	@Autowired private TransactionTemplate transactionTemplate;

	@Autowired private AttachmentService attachmentService;

	@Value("classpath:attachment.png")
	private Resource fileResource;

	private static String attachmentId;

	@Order(1)
	@Test
	public void addAttachment() throws Exception
	{
		var bytes = IOUtils.toByteArray(fileResource.getInputStream());

		var attachment = attachmentService.addAttachment(Objects.requireNonNull(fileResource.getFilename()), MediaType.IMAGE_PNG_VALUE, UserInfo.SYSTEM_USER_ID, bytes);
		var savedData = attachmentService.getAttachmentData(attachment.getId());

		assertEquals(attachment.getSize(), (Integer) bytes.length);
		assertArrayEquals(bytes, IOUtils.toByteArray(savedData.getInputStream()));

		attachmentId = attachment.getId();
	}

	@Order(2)
	@Test
	public void getAttachment() throws Exception
	{
		var attachment = attachmentService.getAttachment(attachmentId);
		var dataResource = attachmentService.getAttachmentData(attachment.getId());
		var dataChecksum = attachmentService.calculateChecksum(IOUtils.toByteArray(dataResource.getInputStream()));

		assertEquals(attachment.getChecksum(), dataChecksum);
	}

	@Order(3)
	@Test
	public void syncStores() throws Exception
	{
		var source = attachmentService.getAttachmentStore();
		var target = attachmentService.getAttachmentStores().stream().filter(it -> !it.equals(source)).findFirst().orElse(null);

		assertNotNull(target);

		var attachment = attachmentService.getAttachment(attachmentId);

		assertThrows(AppException.class, () -> target.getAttachment(attachment));

		attachmentService.syncAttachmentStores(source.getType(), target.getType());

		var dataResource = target.getAttachment(attachment);
		var dataChecksum = attachmentService.calculateChecksum(IOUtils.toByteArray(dataResource.getInputStream()));

		assertEquals(attachment.getChecksum(), dataChecksum);
	}

	@Order(4)
	@Test
	public void deleteAttachment()
	{
		var attachment = attachmentService.getAttachment(attachmentId);
		var attachmentStore = attachmentService.getAttachmentStore();

		attachmentStore.deleteAttachment(attachment);

		assertFalse(attachmentStore.attachmentExists(attachment));
	}

	@Order(5)
	@Test
	public void addAttachmentWithRollback() throws Exception
	{
		var bytes = IOUtils.toByteArray(fileResource.getInputStream());
		var rolledBackAttachmentId = UUID.randomUUID().toString();
		MutableObject<Attachment> attachment = new MutableObject<>();

		try
		{
			transactionTemplate.execute(status -> {
				attachment.setValue(attachmentService.addAttachment(rolledBackAttachmentId, Objects.requireNonNull(fileResource.getFilename()), MediaType.IMAGE_PNG_VALUE, UserInfo.SYSTEM_USER_ID, bytes));

				throw new RuntimeException("rolling back");
			});
		}
		catch (Exception ignore)
		{
			// ignore
		}

		assertFalse(attachmentService.getAttachmentStore().attachmentExists(attachment.getValue()));
	}
}
