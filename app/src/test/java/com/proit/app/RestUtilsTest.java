package com.proit.app;

import com.proit.app.common.AbstractTests;
import com.proit.app.utils.RestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RestUtilsTest extends AbstractTests
{
	private static final String TEST_ADDRESS_URL = "https://1.1.1.1/";

	@Test
	public void testCorrectRequest()
	{
		var restTemplate = RestUtils.createRestTemplate(60);

		var response = restTemplate.getForEntity(TEST_ADDRESS_URL, String.class);

		assertEquals(response.getStatusCode(), HttpStatus.OK);
	}
}
