/*
 *    Copyright 2019-2022 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.proit.app.jms.service;

import com.proit.app.utils.TimeUtils;
import com.proit.app.jms.AbstractTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JmsTransactionTests extends AbstractTests
{
	private final String JMS_TEST_CHANNEL = "testChannel";
	private final int JMS_TIMEOUT_SECONDS = 1;

	@Autowired private TransactionTemplate transactionTemplate;
	@Autowired private JmsTemplate jmsTemplate;

	private String testValue;

	@BeforeEach
	public void beforeTest()
	{
		testValue = null;
	}

	@Test
	public void sendInTransaction()
	{
		var value = UUID.randomUUID().toString();

		transactionTemplate.executeWithoutResult((status) -> {
			jmsTemplate.convertAndSend(JMS_TEST_CHANNEL, value);

			TimeUtils.sleepSeconds(JMS_TIMEOUT_SECONDS);

			assertNull(testValue);
		});

		TimeUtils.sleepSeconds(JMS_TIMEOUT_SECONDS);

		assertEquals(value, testValue);
	}

	@Test
	public void sendNoTransaction()
	{
		assertNull(testValue);

		var value = UUID.randomUUID().toString();

		jmsTemplate.convertAndSend(JMS_TEST_CHANNEL, value);

		TimeUtils.sleepSeconds(JMS_TIMEOUT_SECONDS);

		assertEquals(value, testValue);
	}

	@JmsListener(destination = JMS_TEST_CHANNEL)
	public void handleJmsMessage(String value)
	{
		testValue = value;
	}
}
