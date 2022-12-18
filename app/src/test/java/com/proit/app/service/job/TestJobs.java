package com.proit.app.service.job;

import com.proit.app.model.other.JobResult;
import org.springframework.stereotype.Component;

public class TestJobs
{
	static final String TEST_CRON = "0 0 4 * * *";

	static final String TEST_CRON_RESCHEDULE = "0 0 1 2 1 *";

	static final String TEST_UPDATED_CRON = "0 0 0 1 1 *";
	static final String TEST_DESCRIPTION = "test";

	static final long TEST_FIXED_DELAY = 10000;

	@Component
	static class TestJobWithoutDescription extends AbstractCronJob
	{
		@Override
		public String getCronExpression() { return TEST_CRON; }

		@Override
		protected JobResult execute() { return null; }
	}

	@Component
	@JobDescription
	static class TestJobWithEmptyDescription extends AbstractCronJob
	{
		@Override
		public String getCronExpression() { return TEST_CRON; }

		@Override
		protected JobResult execute() { return null; }
	}

	@Component
	@JobDescription(TEST_DESCRIPTION)
	static class TestJobWithDescription extends AbstractCronJob
	{
		@Override
		public String getCronExpression() { return TEST_CRON; }

		@Override
		protected JobResult execute() { return null; }
	}

	@Component
	static class TestCronJobRescheduling extends AbstractCronJob
	{
		@Override
		public String getCronExpression() { return TEST_CRON_RESCHEDULE; }

		@Override
		protected JobResult execute() { return null; }
	}

	@Component
	static class TestManualJobRescheduling extends AbstractManualJob
	{
		@Override
		protected JobResult execute() { return null; }
	}

	@Component
	static class TestFixedDelayRescheduling extends AbstractFixedDelayJob
	{
		@Override
		protected JobResult execute() { return null; }

		@Override
		public long getFixedDelay()
		{
			return TEST_FIXED_DELAY;
		}
	}
}
