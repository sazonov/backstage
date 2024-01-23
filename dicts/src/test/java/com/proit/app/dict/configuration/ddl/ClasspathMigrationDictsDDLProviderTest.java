package com.proit.app.dict.configuration.ddl;

import com.proit.app.dict.common.CommonTest;
import com.proit.app.dict.common.TestPipeline;
import com.proit.app.dict.configuration.ddl.ClasspathMigrationDictsDDLProvider;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Order(TestPipeline.FIRST_INIT)
public class ClasspathMigrationDictsDDLProviderTest extends CommonTest
{
	public static final String USERS_DICT_NAME = "users";

	@Autowired
	private ClasspathMigrationDictsDDLProvider classpathMigrationDictsDDLProvider;

	@Test
	void updateCorrect()
	{
		classpathMigrationDictsDDLProvider.update();

		assertEquals(USERS_DICT_NAME, dictService.getById(USERS_DICT_NAME).getId());
	}
}
