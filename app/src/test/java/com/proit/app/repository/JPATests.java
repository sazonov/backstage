package com.proit.app.repository;

import com.proit.app.common.AbstractTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class JPATests extends AbstractTests
{
	@Autowired private AuditRepository auditRepository;
	@Autowired private AttachmentRepository attachmentRepository;
	@Autowired private AttachmentBindingRepository attachmentBindingRepository;

	@Test
	public void checkAttachmentRepository()
	{
		attachmentRepository.findAll();
	}

	@Test
	public void checkAttachmentBindingRepository()
	{
		attachmentBindingRepository.findAll();
	}

	@Test
	public void checkAuditRepository()
	{
		auditRepository.findAll();
	}
}
