package com.proit.app.controller;

import com.proit.app.TestMvcApplication;
import com.proit.app.configuration.properties.ApiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = SimpleTestController.class)
@ContextConfiguration(classes = {TestMvcApplication.class})
class GlobalMvcExceptionHandlerTest
{
	@Autowired
	private MockMvc mvc;

	@Autowired
	private ApiProperties apiProperties;

	@Test
	void shouldReturnDefaultMessage() throws Exception
	{
		mvc.perform(get("/test/ping"))
				.andDo(print()).andExpect(status().isOk())
				.andExpect(content().string(containsString("pong")));
	}

	@Test
	void testExceptionThrow() throws Exception
	{
		mvc.perform(get("/test/exception"))
				.andDo(print()).andExpect(status().isOk())
				.andExpect(content().string(containsString("java.lang.Exception")))
				.andExpect(content().string(containsString("exceptionCode")));
	}

	@Test
	void testExceptionThrowWithoutStack() throws Exception
	{
		apiProperties.setStackTraceOnError(false);

		mvc.perform(get("/test/exception"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("exceptionCode")))
				.andExpect(jsonPath("$.stackTrace").doesNotExist());
	}

	@Test
	void testAppExceptionThrow() throws Exception
	{
		mvc.perform(get("/test/appException"))
				.andDo(print()).andExpect(status().isOk())
				.andExpect(content().string(containsString("exceptionCode")));
	}
}