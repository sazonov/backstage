package com.proit.app.service.enums;

import com.proit.app.common.AbstractTests;
import com.proit.app.exception.ObjectNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ComponentScan(basePackageClasses = TestEnums.class)
public class ApiEnumServiceTest extends AbstractTests
{
	@Autowired
	private ApiEnumService apiEnumService;

	@Test
	void getEnumNames_success()
	{
		var expected = Set.of("TestEnum0", "TestEnum1");

		var actual = apiEnumService.getEnumNames();

		assertEquals(expected, actual);
	}

	@Test
	void getEnumDescription_success()
	{
		var expected = Map.of("TEST_VALUE_0", "TEST_VALUE_0", "TEST_VALUE_1", "TEST_VALUE_1");

		var actual = apiEnumService.getEnumDescription("TestEnum0");

		assertEquals(expected, actual);
	}

	@Test
	void getEnumDescription_objectNotFound()
	{
		var expected = ObjectNotFoundException.class;

		Executable actual = () -> apiEnumService.getEnumDescription("NotExistedEnum");

		assertThrows(expected, actual);
	}
}
