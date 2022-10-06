package com.proit.app.service;

import com.proit.app.common.AbstractTests;
import com.proit.app.configuration.jms.JmsBrokerHealthIndicator;
import com.proit.app.configuration.jms.JmsStoreHealthIndicator;
import org.eclipse.persistence.annotations.Array;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JmsBrokerHealthIndicatorTests extends AbstractTests
{
	@Autowired private JmsBrokerHealthIndicator brokerHealthIndicator;
	@Autowired private JmsStoreHealthIndicator storeHealthIndicator;

	@Test
	public void brokerHealth()
	{
		assertEquals(brokerHealthIndicator.health().getStatus(), Status.UP);
	}

	@Test
	public void storeHealth()
	{
		assertEquals(storeHealthIndicator.health().getStatus(), Status.UP);
	}
}
