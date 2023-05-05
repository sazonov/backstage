package com.proit.app.service.job;

import com.proit.app.model.dto.job.EmptyJobParams;
import com.proit.app.model.dto.job.JobParams;
import com.proit.app.model.other.JobResult;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;

public class TestJobs
{
	static final String TEST_CRON = "0 0 4 * * *";

	static final String TEST_CRON_RESCHEDULE = "0 0 1 2 1 *";

	static final String TEST_UPDATED_CRON = "0 0 0 1 1 *";
	static final String TEST_DESCRIPTION = "test";

	static final long TEST_FIXED_DELAY = 10000;

	@Component
	static class TestJobWithoutDescription extends AbstractCronJob<EmptyJobParams>
	{
		@Override
		public String getCronExpression()
		{
			return TEST_CRON;
		}

		@Override
		protected JobResult execute()
		{
			return null;
		}
	}

	@Component
	@JobDescription
	static class TestJobWithEmptyDescription extends AbstractCronJob<EmptyJobParams>
	{
		@Override
		public String getCronExpression()
		{
			return TEST_CRON;
		}

		@Override
		protected JobResult execute()
		{
			return null;
		}
	}

	@Component
	@JobDescription(TEST_DESCRIPTION)
	static class TestJobWithDescription extends AbstractCronJob<EmptyJobParams>
	{
		@Override
		public String getCronExpression()
		{
			return TEST_CRON;
		}

		@Override
		protected JobResult execute()
		{
			return null;
		}
	}

	@Component
	static class TestCronJobRescheduling extends AbstractCronJob<EmptyJobParams>
	{
		@Override
		public String getCronExpression()
		{
			return TEST_CRON_RESCHEDULE;
		}

		@Override
		protected JobResult execute()
		{
			return null;
		}
	}

	@Component
	static class TestManualJobRescheduling extends AbstractManualJob<EmptyJobParams>
	{
		@Override
		protected JobResult execute()
		{
			return null;
		}
	}

	@Component
	static class TestFixedDelayRescheduling extends AbstractFixedDelayJob<EmptyJobParams>
	{
		@Override
		protected JobResult execute()
		{
			return null;
		}

		@Override
		public long getFixedDelay()
		{
			return TEST_FIXED_DELAY;
		}
	}

	@Component
	static class TestManualJobWithParams extends AbstractManualJob<TestManualJobParams>
	{
		@Override
		protected JobResult execute(TestManualJobParams params)
		{
			return JobResult.builder()
					.properties(Map.of("result", params.testParam))
					.build();
		}

		@Override
		protected TestManualJobParams createDefaultParams()
		{
			return new TestManualJobParams("defaultValue");
		}
	}

	@Getter
	@EqualsAndHashCode
	@AllArgsConstructor
	static class TestManualJobParams implements JobParams
	{
		private String testParam;
	}
}
