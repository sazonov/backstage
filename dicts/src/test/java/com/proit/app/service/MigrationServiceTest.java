package com.proit.app.service;

import com.proit.app.common.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.AbstractMap;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MigrationServiceTest extends AbstractTest
{
	@Autowired
	private MigrationService migrationService;

	@Test
	void migrateExc()
	{
		assertThrows(RuntimeException.class, () -> migrationService.migrate(new AbstractMap.SimpleEntry<>("incorrectName", null)));
	}
}