package com.proit.app.configuration.ddl;

import com.proit.app.common.AbstractTest;
import com.proit.app.service.DictService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DictsDDLConfigurationTest extends AbstractTest
{
	public static final String USERS_DICT_NAME = "users";

	@Autowired
	private DictService dictService;
	@Autowired
	private DictsDDLProvider dictsDDLProvider;

	@Test
	@Order(2)
	void updateCorrect()
	{
		dictsDDLProvider.update();

		assertEquals(USERS_DICT_NAME, dictService.getById(USERS_DICT_NAME).getId());
	}

	@Test
	@Order(1)
	@Transactional
	void updateTransactionalExc()
	{
		assertThrows(RuntimeException.class, () -> dictsDDLProvider.update());
	}
}
