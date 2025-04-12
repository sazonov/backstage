package com.proit.app.database;

import com.proit.app.database.domain.Ticket;
import com.proit.app.database.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты проверяют работоспособность конвертации поддерживаемых типов даты-времени.
 */
public class DateTimeConversionTest extends AbstractTest
{
	@Autowired
	TicketRepository ticketRepository;

	@Test
	void selectExistsDateTimeEntity()
	{
		assertDoesNotThrow(() -> ticketRepository.findAll());
	}

	@Test
	void insertDateTimeEntity()
	{
		var ticketCreated = LocalDateTime.of(2021, 8, 15, 6, 0, 0);

		var sourceTicket = new Ticket();
		sourceTicket.setCreated(ticketCreated);
		sourceTicket.setSenderEmail("email");

		var ticket = ticketRepository.save(sourceTicket);

		assertEquals(ticketCreated, ticket.getCreated());
	}
}
