package com.proit.app.jobs.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proit.app.jobs.TestApplication;
import com.proit.app.jobs.configuration.SchedulerConfiguration;
import com.proit.app.jobs.data.TestJobs;
import com.proit.app.jobs.service.JobManager;
import com.proit.app.model.other.exception.ApiStatusCodeImpl;
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

import java.util.HashMap;
import java.util.Map;

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
	private MockMvc mvc;

	@Autowired
	private ObjectMapper mapper;

	@Test
	void executeWithCorrectParams() throws Exception
	{
		var params = Map.of("testParam", "param");

		mvc.perform(post("/api/scheduledJobs/testJobs.TestManualJobWithParams/execute")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(params)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("message").value(Matchers.is(ApiStatusCodeImpl.OK.getMessage())))
				.andDo(print());
	}

	@Test
	void executeWithNullParamValue() throws Exception
	{
		var params = new HashMap<String, Object>(1);
		params.put("testParam", null);

		mvc.perform(post("/api/scheduledJobs/testJobs.TestManualJobWithParams/execute")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(params)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("message").value(Matchers.is(ApiStatusCodeImpl.OK.getMessage())))
				.andDo(print());
	}

	@Test
	void executeWithNullParams() throws Exception
	{
		mvc.perform(post("/api/scheduledJobs/testJobs.TestManualJobWithParams/execute")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("message").value(Matchers.is(ApiStatusCodeImpl.OK.getMessage())))
				.andDo(print());
	}

	@Test
	void executeWitEmptyParams() throws Exception
	{
		mvc.perform(post("/api/scheduledJobs/testJobs.TestManualJobWithParams/execute")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(Map.of())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("message").value(Matchers.is(ApiStatusCodeImpl.OK.getMessage())))
				.andDo(print());
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
}