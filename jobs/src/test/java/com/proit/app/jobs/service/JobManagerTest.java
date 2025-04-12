package com.proit.app.jobs.service;

import com.google.common.collect.Ordering;
import com.proit.app.exception.AppException;
import com.proit.app.jobs.AbstractTests;
import com.proit.app.jobs.data.TestJobs;
import com.proit.app.jobs.model.dto.JobTriggerType;
import com.proit.app.model.other.exception.ApiStatusCodeImpl;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ComponentScan(basePackageClasses = TestJobs.class)
class JobManagerTest extends AbstractTests
{
	@Autowired
	private JobManager jobManager;

	@Test
	void getJobList_checkCountTest()
	{
		assertEquals(8, jobManager.getJobList().size());
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

		Assertions.assertEquals(TestJobs.TEST_DESCRIPTION, jobDescription);
	}

	@Test
	void rescheduleCronJob_checkReschedule()
	{
		var jobName = "testJobs.TestCronJobRescheduling";

		var jobEventListener = (JobHealthIndicator) jobManager.getJobs()
				.get(jobName)
				.getEventListener();

		var oldNextExecutionTime = jobEventListener.getNextExecutionTime();

		var cronUpdatedNextExecutionTime = getNextExecutionDate(TestJobs.TEST_UPDATED_CRON);

		var cronOldNextExecutionTime = getNextExecutionDate(TestJobs.TEST_CRON_RESCHEDULE);

		jobManager.rescheduleJob(jobName, JobTriggerType.CRON.createTrigger(TestJobs.TEST_UPDATED_CRON));

		assertEquals(cronOldNextExecutionTime, oldNextExecutionTime);
		assertEquals(cronUpdatedNextExecutionTime, jobEventListener.getNextExecutionTime());
		assertNotEquals(oldNextExecutionTime, jobEventListener.getNextExecutionTime());
	}

	@Test
	void rescheduleManualJob_checkReschedule()
	{
		var jobName = "testJobs.TestManualJobRescheduling";

		var jobEventListener = (JobHealthIndicator) jobManager.getJobs()
				.get(jobName)
				.getEventListener();

		var oldNextExecutionTime = jobEventListener.getNextExecutionTime();

		var cronUpdatedNextExecutionTime = getNextExecutionDate(TestJobs.TEST_UPDATED_CRON);

		jobManager.rescheduleJob(jobName, JobTriggerType.CRON.createTrigger(TestJobs.TEST_UPDATED_CRON));

		assertNull(oldNextExecutionTime);
		assertEquals(cronUpdatedNextExecutionTime, jobEventListener.getNextExecutionTime());
	}

	@Test
	void rescheduleFixedDelay_checkReschedule()
	{
		var jobName = "testJobs.TestFixedDelayRescheduling";

		var jobEventListener = (JobHealthIndicator) jobManager.getJobs()
				.get(jobName)
				.getEventListener();

		var oldNextExecutionTime = jobEventListener.getNextExecutionTime();

		jobManager.rescheduleJob(jobName, JobTriggerType.INTERVAL.createTrigger(String.valueOf(TestJobs.TEST_FIXED_DELAY)));

		assertNotEquals(oldNextExecutionTime, jobEventListener.getNextExecutionTime());
	}

	@Test
	void rescheduleJob_notExistingJob()
	{
		var jobName = "testJobs.notExistingJob";

		AppException thrown = assertThrows(AppException.class, () -> jobManager.rescheduleJob(jobName, JobTriggerType.CRON.createTrigger(TestJobs.TEST_UPDATED_CRON)));

		Assertions.assertEquals(ApiStatusCodeImpl.OBJECT_NOT_FOUND, thrown.getStatus());
	}

	@Test
	void rescheduleJob_badCronExpression()
	{
		var jobName = "testJobs.TestManualJobRescheduling";

		AppException thrown = assertThrows(AppException.class, () -> jobManager.rescheduleJob(jobName, JobTriggerType.INTERVAL.createTrigger("*")));

		Assertions.assertEquals(ApiStatusCodeImpl.ILLEGAL_INPUT, thrown.getStatus());
	}

	@Test
	void rescheduleJob_badDelayExpression()
	{
		var jobName = "testJobs.TestManualJobRescheduling";

		AppException thrown = assertThrows(AppException.class, () -> jobManager.rescheduleJob(jobName, JobTriggerType.CRON.createTrigger("*")));

		Assertions.assertEquals(ApiStatusCodeImpl.ILLEGAL_INPUT, thrown.getStatus());
	}

	@Test
	void execute_withoutParams_success()
	{
		var actualResult = jobManager.executeJobAndWait(TestJobs.TestManualJobWithParams.class, null)
				.getProperties()
				.get("result");

		assertEquals("defaultValue", actualResult);
	}

	@Test
	void execute_withParams_success()
	{
		var params = new TestJobs.TestManualJobParams("testValue");

		var actualResult = jobManager.executeJobAndWait(TestJobs.TestManualJobWithParams.class, params)
				.getProperties()
				.get("result");

		Assertions.assertEquals(params.getTestParam(), actualResult);
	}

	@Test
	void getParams_success()
	{
		var jobName = "testJobs.TestManualJobWithParams";

		var actualResult = jobManager.getParams(jobName);

		Assertions.assertEquals(new TestJobs.TestManualJobParams("defaultValue"), actualResult);
	}

	@Test
	void getParams_notExistingJob()
	{
		var jobName = "testJobs.notExistingJob";

		AppException thrown = assertThrows(AppException.class, () -> jobManager.getParams(jobName));

		Assertions.assertEquals(ApiStatusCodeImpl.OBJECT_NOT_FOUND, thrown.getStatus());
	}

	private Date getNextExecutionDate(String cronExpression)
	{
		return Date.from(CronExpression.parse(cronExpression).next(LocalDateTime.now())
				.atZone(ZoneId.systemDefault())
				.toInstant());
	}
}