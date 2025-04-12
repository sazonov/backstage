package com.proit.app.jms.service;

import com.proit.app.jms.TestApplication;
import org.apache.activemq.broker.BrokerService;
import org.junit.ClassRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.activemq.ActiveMQContainer;

import static org.junit.jupiter.api.Assertions.assertNull;

@ContextConfiguration(classes = TestApplication.class, initializers = JmsExternalBrokerTest.Initializer.class)
public class JmsExternalBrokerTest extends JmsTransactionTests
{
	public static final String ACTIVEMQ_IMAGE_NAME = "apache/activemq-classic:5.18.3";

	@ClassRule
	public static final ActiveMQContainer activeMQ;

	static
	{
		activeMQ = new ActiveMQContainer(ACTIVEMQ_IMAGE_NAME);
		activeMQ.start();
	}

	static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext>
	{
		@Override
		public void initialize(ConfigurableApplicationContext configurableApplicationContext)
		{
			TestPropertyValues.of(
					"app.jms.brokerType=EXTERNAL",
					"app.jms.brokerUrl=" + activeMQ.getBrokerUrl()
			).applyTo(configurableApplicationContext.getEnvironment());
		}
	}

	@Autowired(required = false)
	BrokerService brokerService;

	@Test
	void noBrokerService()
	{
		assertNull(brokerService);
	}
}
