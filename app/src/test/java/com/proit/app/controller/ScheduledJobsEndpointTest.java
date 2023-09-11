package com.proit.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proit.app.TestMvcApplication;
import com.proit.app.configuration.SchedulerConfiguration;
import com.proit.app.endpoint.ScheduledJobsEndpoint;
import com.proit.app.model.api.ApiStatusCodeImpl;
import com.proit.app.service.job.JobManager;
import com.proit.app.service.job.TestJobs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ScheduledJobsEndpoint.class)
@ContextConfiguration(classes = {TestMvcApplication.class, SchedulerConfiguration.class, ScheduledJobsEndpoint.class})
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
				.andExpect(jsonPath("message").value(is(ApiStatusCodeImpl.OK.getMessage())))
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
				.andExpect(jsonPath("message").value(is(ApiStatusCodeImpl.OK.getMessage())))
				.andDo(print());
	}

	@Test
	void executeWithNullParams() throws Exception
	{
		mvc.perform(post("/api/scheduledJobs/testJobs.TestManualJobWithParams/execute")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("message").value(is(ApiStatusCodeImpl.OK.getMessage())))
				.andDo(print());
	}

	@Test
	void executeWitEmptyParams() throws Exception
	{
		mvc.perform(post("/api/scheduledJobs/testJobs.TestManualJobWithParams/execute")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(Map.of())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("message").value(is(ApiStatusCodeImpl.OK.getMessage())))
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
				.andExpect(jsonPath("message").value(is(ApiStatusCodeImpl.ILLEGAL_INPUT.getMessage())))
				.andDo(print());
	}
}