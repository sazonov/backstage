package com.proit.app.configuration.ddl;

import com.proit.app.common.AbstractTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DictsDDLConfigurationTest extends AbstractTest
{
	public static final String USERS_DICT_NAME = "users";

	@Autowired
	private DictsDDLProvider dictsDDLProvider;

	@Test
	void updateCorrect()
	{
		dictsDDLProvider.update();

		assertEquals(USERS_DICT_NAME, dictService.getById(USERS_DICT_NAME).getId());
	}
}
