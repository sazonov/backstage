package com.proit.app.jobs.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proit.app.jobs.TestApplication;
import com.proit.app.jobs.configuration.SchedulerConfiguration;
import com.proit.app.jobs.data.TestJobs;
import com.proit.app.jobs.service.JobHealthIndicator;
import com.proit.app.jobs.service.JobManager;
import com.proit.app.model.other.exception.ApiStatusCodeImpl;
import com.proit.app.utils.TaskUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ScheduledJobsEndpoint.class)
@ContextConfiguration(classes = {TestApplication.class, SchedulerConfiguration.class, ScheduledJobsEndpoint.class})
@ImportAutoConfiguration(exclude = SecurityAutoConfiguration.class)
@ComponentScan(basePackageClasses = {TestJobs.class, JobManager.class})
class ScheduledJobsEndpointTest
{
	@Autowired
	private JobManager jobManager;

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper mapper;

	@Test
	void executeNoParamJobWithSomeParams() throws Exception
	{
		var params = Map.of("testParam", "param");

		executeJob("testJobs.TestFixedDelayRescheduling", params);
	}

	@Test
	void executeWithCorrectParams() throws Exception
	{
		var params = Map.of("testParam", "param");

		executeJob("testJobs.TestManualJobWithParams", params);
	}

	@Test
	void executeWithNullParamValue() throws Exception
	{
		var params = new HashMap<String, Object>(1);
		params.put("testParam", null);

		executeJob("testJobs.TestManualJobWithParams", params);
	}

	@Test
	void executeWithNullParams() throws Exception
	{
		executeJob("testJobs.TestManualJobWithParams", null);
	}

	@Test
	void executeWitEmptyParams() throws Exception
	{
		executeJob("testJobs.TestManualJobWithParams", Map.of());
	}

	@Test
	void executeWithInvalidBody() throws Exception
	{
		var params = Map.of("testInteger", -1);

		mvc.perform(post("/api/scheduledJobs/testJobs.TestManualJobWithValidationParams/execute")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(params)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("message").value(Matchers.is(ApiStatusCodeImpl.ILLEGAL_INPUT.getMessage())))
				.andDo(print());
	}

	private <K, V> void executeJob(String jobName, Map<K, V> params) throws Exception
	{
		var jobHealthIndicator = (JobHealthIndicator) jobManager.getJobs()
				.get(jobName)
				.getEventListener();

		var lastCompletionTime = jobHealthIndicator.getLastCompletionTime();

		var content = params != null ? mapper.writeValueAsString(params).getBytes(StandardCharsets.UTF_8) : null;

		mvc.perform(post("/api/scheduledJobs/%s/execute".formatted(jobName))
						.contentType(MediaType.APPLICATION_JSON)
						.content(content))
				.andExpect(status().isOk())
				.andExpect(jsonPath("message").value(Matchers.is(ApiStatusCodeImpl.OK.getMessage())))
				.andDo(print());

		assertTrue(TaskUtils.testWithTryCount(5, 300, () -> lastCompletionTime != jobHealthIndicator.getLastCompletionTime()));
	}
}