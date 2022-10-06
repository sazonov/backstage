package com.proit.app.service;

import com.proit.app.common.AbstractTests;
import com.proit.app.exception.AppException;
import com.proit.app.model.other.user.UserInfo;
import com.proit.app.service.attachment.AttachmentService;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AttachmentServiceTests extends AbstractTests
{
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
}
