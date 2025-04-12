package com.proit.app.audit.service;

import com.proit.app.audit.AbstractTests;
import com.proit.app.audit.model.other.AuditEventBuilder;
import com.proit.app.audit.model.other.AuditFilter;
import com.proit.app.audit.repository.AuditRepository;
import com.proit.app.utils.TimeUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JpaAuditStoreTest extends AbstractTests
{
	@Autowired
	private AuditStore auditStore;

	@Autowired
	private AuditRepository auditRepository;

	@BeforeAll
	void initTestData()
	{
		Stream.of(AuditEventBuilder.create(AuditServiceTests.TestEventTypes.EVENT_TYPE_3, AuditServiceTests.TestData.USER_2_ID),
						AuditEventBuilder.create(AuditServiceTests.TestEventTypes.EVENT_TYPE_3, AuditServiceTests.TestData.USER_2_ID, AuditServiceTests.TestData.USER_1_ID),
						AuditEventBuilder.create(AuditServiceTests.TestEventTypes.EVENT_TYPE_3, AuditServiceTests.TestData.USER_1_ID, AuditServiceTests.TestData.USER_2_ID),
						AuditEventBuilder.create(AuditServiceTests.TestEventTypes.EVENT_TYPE_2, AuditServiceTests.TestData.USER_1_ID, AuditServiceTests.TestData.ADMIN_ID),
						AuditEventBuilder.create(AuditServiceTests.TestEventTypes.EVENT_TYPE_1, AuditServiceTests.TestData.USER_1_ID, AuditServiceTests.TestData.ADMIN_ID),
						AuditEventBuilder.create(AuditServiceTests.TestData.TYPE_1, AuditServiceTests.TestData.ADMIN_ID),
						AuditEventBuilder.create(AuditServiceTests.TestData.TYPE_2, AuditServiceTests.TestData.ADMIN_ID, AuditServiceTests.TestData.ADMIN_ID))
				.map(AuditEventBuilder::build)
				.forEach(auditStore::write);

		TimeUtils.sleepSeconds(2);
	}

	@AfterAll
	void tearDown()
	{
		auditRepository.deleteAll();
	}

	@Test
	void getByFilter()
	{
		var actual = auditStore.getByFilter(AuditFilter.builder().build(), Pageable.unpaged());

		assertEquals(7, actual.getTotalElements());
	}
}