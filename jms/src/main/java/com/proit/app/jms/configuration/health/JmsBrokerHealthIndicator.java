/*
 *    Copyright 2019-2024 the original author or authors.
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

package com.proit.app.jms.configuration.health;

import com.proit.app.jms.configuration.conditional.ConditionalOnEmbeddedBroker;
import com.proit.app.service.health.AbstractHealthIndicator;
import lombok.Cleanup;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQDestination;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConditionalOnEmbeddedBroker
public class JmsBrokerHealthIndicator extends AbstractHealthIndicator
{
	public static final String DEFAULT_AMQ_PREFIX = "ActiveMQ.Statistics.Destination.";
	private static final long RECEIVE_TIMEOUT = 1000;
	private final BrokerService brokerService;

	private final JmsTemplate jmsTemplate;

	public JmsBrokerHealthIndicator(BrokerService brokerService, @Qualifier("nonCachingJmsConnectionFactory") ConnectionFactory connectionFactory)
	{
		this.brokerService = brokerService;

		jmsTemplate = new JmsTemplate(connectionFactory);
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception
	{
		var destinations = brokerService.getBroker().getDestinations();

		Arrays.stream(destinations)
				.filter(it -> !it.getPhysicalName().contains("ActiveMQ"))
				.forEach(destination -> jmsTemplate.execute(
						session -> buildHealthByDestinationType(builder, destination, session), true));

		builder.up();
	}

	private Object buildHealthByDestinationType(Health.Builder builder, ActiveMQDestination destination, Session session) throws JMSException
	{
		if (destination.isQueue())
		{
			buildHealthForQueue(builder, destination, session);
		}
		else if (destination.isTopic())
		{
			buildHealthForTopic(builder, destination, session);
		}

		return null;
	}

	private void buildHealthForTopic(Health.Builder builder, ActiveMQDestination destination, Session session) throws JMSException
	{
		var replyTo = session.createTemporaryTopic();

		@Cleanup var consumer = session.createConsumer(replyTo);

		var queueName = DEFAULT_AMQ_PREFIX + destination.getPhysicalName();
		var testQueue = session.createTopic(queueName);

		@Cleanup var producer = session.createProducer(testQueue);

		var msg = session.createMessage();
		msg.setJMSReplyTo(replyTo);

		producer.send(msg);

		buildHealthDetails(builder, consumer, destination);
	}

	private void buildHealthForQueue(Health.Builder builder, ActiveMQDestination destination, Session session) throws JMSException
	{
		var replyTo = session.createTemporaryQueue();

		@Cleanup var consumer = session.createConsumer(replyTo);

		var queueName = DEFAULT_AMQ_PREFIX + destination.getPhysicalName();
		var testQueue = session.createQueue(queueName);

		@Cleanup var producer = session.createProducer(testQueue);

		var msg = session.createMessage();
		msg.setJMSReplyTo(replyTo);

		producer.send(msg);

		buildHealthDetails(builder, consumer, destination);
	}

	private void buildHealthDetails(Health.Builder builder, MessageConsumer consumer, ActiveMQDestination destination)
	{
		try
		{
			var reply = (MapMessage) consumer.receive(RECEIVE_TIMEOUT);
			reply.acknowledge();

			var names = reply.getMapNames();
			var properties = new LinkedHashMap<String, Object>();

			while (names.hasMoreElements())
			{
				var name = names.nextElement().toString();

				properties.put(destination.getPhysicalName() + "." + name, reply.getObject(name));
			}

			builder.withDetails(properties);
		}
		catch (Exception e)
		{
			builder.withDetails(Map.of(destination.getPhysicalName() + ".exception", e.getMessage()));
		}
	}
}
