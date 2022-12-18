package com.proit.app.repository;

import com.proit.app.common.AbstractTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class JPATests extends AbstractTests
{
	@Autowired private AuditRepository auditRepository;
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

	@Test
	void checkAuditRepository()
	{
		auditRepository.findAll();
	}
}
