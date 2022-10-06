package com.proit.app.service;

import com.proit.app.common.AbstractTests;
import com.proit.app.model.other.audit.AuditEventBuilder;
import com.proit.app.model.other.filter.AuditFilter;
import com.proit.app.service.audit.AuditService;
import com.proit.app.utils.TimeUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuditServiceTests extends AbstractTests
{
	@Autowired
	private AuditService auditService;

	@BeforeAll
	void initTestData()
	{
		Stream.of(AuditEventBuilder.create(TestEventTypes.EVENT_TYPE_3, TestData.USER_2_ID),
				AuditEventBuilder.create(TestEventTypes.EVENT_TYPE_3, TestData.USER_2_ID, TestData.USER_1_ID),
				AuditEventBuilder.create(TestEventTypes.EVENT_TYPE_3, TestData.USER_1_ID, TestData.USER_2_ID),
				AuditEventBuilder.create(TestEventTypes.EVENT_TYPE_2, TestData.USER_1_ID, TestData.ADMIN_ID),
				AuditEventBuilder.create(TestEventTypes.EVENT_TYPE_1, TestData.USER_1_ID, TestData.ADMIN_ID),
				AuditEventBuilder.create(TestData.TYPE_1, TestData.ADMIN_ID),
				AuditEventBuilder.create(TestData.TYPE_2, TestData.ADMIN_ID, TestData.ADMIN_ID))
				.map(AuditEventBuilder::build)
				.forEach(auditService::log);

		TimeUtils.sleepSeconds(2);

		var actual = auditService.getByFilter(AuditFilter.builder().build(), Pageable.unpaged());

		assertEquals(7, actual.getTotalElements());
	}

	@Test
	public void getByFilter_emptyFilterTestData()
	{
		var actual = auditService.getByFilter(AuditFilter.builder().build(), Pageable.unpaged());

		assertEquals(7, actual.getTotalElements());
	}

	@Test
	public void getByFilter_nullFilterNullPage()
	{
		assertThrows(NullPointerException.class, () -> auditService.getByFilter(null, null));
	}

	@Test
	public void getByFilter_filterWithObjectId()
	{
		var testFilter = AuditFilter.builder()
				.objectId(TestData.USER_1_ID)
				.build();

		var actual = auditService.getByFilter(testFilter, PageRequest.of(0, Integer.MAX_VALUE))
				.get()
				.toList();

		assertEquals(3, actual.size());
	}

	@Test
	public void getByFilter_FilterWithObjectIdAndTypes()
	{
		var testFilter = AuditFilter.builder()
				.objectId(TestData.USER_1_ID)
				.types(List.of(TestData.TYPE_1))
				.build();

		var actual = auditService.getByFilter(testFilter, Pageable.unpaged())
				.get()
				.toList();

		assertEquals(0, actual.size());
	}

	@Test
	public void getByFilter_FilterWithObjectIdAndTypesAndUserId()
	{
		var testFilter = AuditFilter.builder()
				.objectId(TestData.USER_1_ID)
				.userId(TestData.ADMIN_ID)
				.types(List.of(TestEventTypes.EVENT_TYPE_1.name()))
				.build();

		var actual = auditService.getByFilter(testFilter, Pageable.unpaged())
				.get()
				.toList();

		assertEquals(1, actual.size());
	}

	@Test
	public void log_auditEventNullParam()
	{
		assertThrows(Exception.class, () -> auditService.log(AuditEventBuilder.create((String) null, null, null).build()));
	}

	@Test
	public void log_nullAuditEvent()
	{
		assertThrows(NullPointerException.class, () -> auditService.log(null));
	}

	@Test
	public void getByFilter_FilterWithObjectIdAndEnumTypesAndUserId()
	{
		var testFilter = AuditFilter.builder()
				.objectId(TestData.USER_2_ID)
				.userId(TestData.USER_1_ID)
				.types(List.of(TestEventTypes.EVENT_TYPE_3.name()))
				.build();

		var actual = auditService.getByFilter(testFilter, Pageable.unpaged())
				.get()
				.toList();

		assertEquals(1, actual.size());
	}

	enum TestEventTypes
	{
		EVENT_TYPE_1,
		EVENT_TYPE_2,
		EVENT_TYPE_3,
	}

	interface TestData
	{
		String USER_1_ID = "user1_id";
		String USER_2_ID = "user2_id";
		String ADMIN_ID = "admin_id";
		String TYPE_1 = "TYPE_1";
		String TYPE_2 = "TYPE_2";
	}
}
