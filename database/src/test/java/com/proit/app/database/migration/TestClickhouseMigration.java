package com.proit.app.database.migration;

import com.proit.app.database.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestClickhouseMigration extends AbstractTest
{
	@Autowired
	@Qualifier("clickHouseJdbcTemplate")
	NamedParameterJdbcTemplate clickhouse;

	@Test
	void checkIfTestTableExistsTest()
	{
		var result = clickhouse.queryForObject("exists table test", Map.of(), Integer.class);

		assertEquals(1, result);
	}

	@Test
	void checkIfRecordsInTestTableExistsTest()
	{
		var result = clickhouse.queryForList("select * from test", Map.of());

        assertFalse(result.isEmpty());
	}
}
