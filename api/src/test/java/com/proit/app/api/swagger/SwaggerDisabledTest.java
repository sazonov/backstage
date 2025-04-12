package com.proit.app.api.swagger;

import com.proit.app.api.TestSwaggerApplication;
import com.proit.app.api.configuration.properties.ApiProperties;
import org.junit.jupiter.api.Test;
import org.springdoc.webmvc.api.OpenApiWebMvcResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = OpenApiWebMvcResource.class)
@ContextConfiguration(classes = {TestSwaggerApplication.class})
class SwaggerDisabledTest
{
	@Autowired
	private MockMvc mvc;

	@Test
	void testSwaggerIsDisabled() throws Exception
	{
		mvc.perform(get("/v3/api-docs"))
				.andDo(print())
				.andExpect(status().is4xxClientError());
	}

	@DynamicPropertySource
	static void swaggerProperties(DynamicPropertyRegistry registry)
	{
		registry.add(ApiProperties.SWAGGER_ACTIVATION_PROPERTY, () -> "false");
	}
}