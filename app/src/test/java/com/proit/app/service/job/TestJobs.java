package com.proit.app.service.job;

import com.proit.app.model.other.JobResult;
import org.springframework.stereotype.Component;

public class TestJobs
{
	static final String TEST_CRON = "0 0 4 * * *";
	static final String TEST_DESCRIPTION = "test";

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
}
