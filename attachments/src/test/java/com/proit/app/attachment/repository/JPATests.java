package com.proit.app.attachment.repository;

import com.proit.app.attachment.AbstractTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class JPATests extends AbstractTests
{
	@Autowired private AttachmentRepository attachmentRepository;
	@Autowired private AttachmentBindingRepository attachmentBindingRepository;

	@Test
	void checkAttachmentRepository()
	{
		attachmentRepository.findAll();
	}

	@Test
	void checkAttachmentBindingRepository()
	{
		attachmentBindingRepository.findAll();
	}
}
