package com.proit.app.audit.repository;

import com.proit.app.audit.AbstractTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class JPATest extends AbstractTests
{
	@Autowired
	private AuditRepository auditRepository;

	@Test
	void checkAuditRepository()
	{
		auditRepository.findAll();
	}
}
