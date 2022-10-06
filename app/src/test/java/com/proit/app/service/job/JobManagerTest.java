package com.proit.app.service.job;

import com.google.common.collect.Ordering;
import com.proit.app.common.AbstractTests;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ComponentScan(basePackageClasses = TestJobs.class)
class JobManagerTest extends AbstractTests
{
	@Autowired
	private JobManager jobManager;

	@Test
	void getJobList_checkCountTest()
	{
		assertEquals(3, jobManager.getJobList().size());
	}

	@Test
	void getJobList_checkOrderingTest()
	{
		Set<String> ids = jobManager.getJobList().keySet();
		boolean isOrdered = Ordering.natural().isOrdered(ids);

		assertTrue(isOrdered);
	}

	@Test
	void getJobList_withDescriptionCountTest()
	{
		long actual = jobManager.getJobList().values().stream().filter(StringUtils::isNotEmpty).count();

		assertEquals(1, actual);
	}

	@Test
	void getJobList_checkDescriptionTest()
	{
		String jobDescription = jobManager.getJobList().get("testJobs.TestJobWithDescription");

		assertEquals(TestJobs.TEST_DESCRIPTION, jobDescription);
	}
}