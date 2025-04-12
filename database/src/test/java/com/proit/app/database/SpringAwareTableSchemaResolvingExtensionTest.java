package com.proit.app.database;

import com.proit.app.database.domain.Customer;
import com.proit.app.database.domain.Product;
import com.proit.app.database.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты на работоспособность динамической конфигурации схемы БД для Entity
 *
 * @see com.proit.app.database.configuration.jpa.eclipselink.uuid.UuidSupportExtension
 */
public class SpringAwareTableSchemaResolvingExtensionTest extends AbstractTest
{
	@Autowired
	TicketRepository ticketRepository;

	@Autowired
	NamedParameterJdbcTemplate jdbcTemplate;

	@Test
	void selectEntityWithSpelSpecifiedScheme()
	{
		assertDoesNotThrow(() ->
				jdbcTemplate.queryForList("select * from test_scheme.customer", new MapSqlParameterSource(), Customer.class));
	}

	@Test
	void selectEntityWithPlaceholderSpecifiedScheme()
	{
		assertDoesNotThrow(() ->
				jdbcTemplate.queryForList("select * from test_scheme.product", new MapSqlParameterSource(), Product.class));
	}

	@Test
	void selectEntityWithMultiplyTableSpelSpecifiedScheme()
	{
		var tickets = ticketRepository.findAll();

		assertFalse(tickets.isEmpty());

		var ticket = tickets.get(0);

		assertNotNull(ticket.getTitle());
		assertNotNull(ticket.getDescription());
		assertNotNull(ticket.getSenderEmail());
	}
}
