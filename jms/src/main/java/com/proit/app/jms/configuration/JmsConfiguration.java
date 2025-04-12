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

package com.proit.app.jms.configuration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proit.app.jms.configuration.conditional.ConditionalOnJms;
import com.proit.app.jms.configuration.properties.JmsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.transaction.PlatformTransactionManager;

import javax.jms.ConnectionFactory;
import java.util.Optional;

@Slf4j
@Configuration
@EnableJms
@EnableConfigurationProperties(JmsProperties.class)
@ConditionalOnJms
@RequiredArgsConstructor
public class JmsConfiguration
{
	private static final String DEFAULT_CONCURRENCY = "1-5";

	private final JmsProperties jmsProperties;

	@Bean
	@Primary
	public JmsListenerContainerFactory<?> jmsListenerContainerFactory(ConnectionFactory connectionFactory, PlatformTransactionManager transactionManager)
	{
		return createListenerContainerFactory(connectionFactory, transactionManager);
	}

	@Bean
	public JmsListenerContainerFactory<?> nonTxJmsListenerContainerFactory(ConnectionFactory connectionFactory)
	{
		return createListenerContainerFactory(connectionFactory, null);
	}

	private JmsListenerContainerFactory<?> createListenerContainerFactory(ConnectionFactory connectionFactory, PlatformTransactionManager transactionManager)
	{
		var factory = new DefaultJmsListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setAutoStartup(true);
		factory.setConcurrency(DEFAULT_CONCURRENCY);
		factory.setSessionTransacted(false);

		if (jmsProperties.isJacksonMessageConverterEnabled())
		{
			factory.setMessageConverter(jacksonMessageConverter());
		}

		if (transactionManager != null)
		{
			factory.setSessionTransacted(true);
			factory.setTransactionManager(transactionManager);
		}

		return factory;
	}

	@Bean
	@Primary
	public ConnectionFactory jmsConnectionFactory(Optional<BrokerService> broker)
	{
		CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(createConnectionFactory());
		cachingConnectionFactory.setSessionCacheSize(10);
		cachingConnectionFactory.setCacheConsumers(false);

		return cachingConnectionFactory;
	}

	@Bean
	public ConnectionFactory nonCachingJmsConnectionFactory(Optional<BrokerService> broker)
	{
		return createConnectionFactory();
	}

	private ConnectionFactory createConnectionFactory()
	{
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(jmsProperties.getBrokerUrl());
		connectionFactory.setTrustAllPackages(true);

		for (String channel : jmsProperties.getChannels().keySet())
		{
			JmsProperties.ChannelPolicy settings = jmsProperties.getChannels().get(channel);

			RedeliveryPolicy channelPolicy = new RedeliveryPolicy();

			channelPolicy.setRedeliveryDelay(settings.getRetryInterval());
			channelPolicy.setMaximumRedeliveryDelay(settings.getRetryInterval() * 10);
			channelPolicy.setMaximumRedeliveries(settings.getTryCount());
			channelPolicy.setUseExponentialBackOff(true);

			connectionFactory.getRedeliveryPolicyMap().put(
					settings.getChannelType().equals(JmsProperties.ChannelType.TOPIC) ? new ActiveMQTopic(channel) : new ActiveMQQueue(channel), channelPolicy);
		}

		return connectionFactory;
	}

	@Bean
	public JmsTransactionManager jmsTransactionManager(ConnectionFactory connectionFactory)
	{
		return new JmsTransactionManager(connectionFactory);
	}

	@Bean
	@Primary
	public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory)
	{
		var jmsTemplate = new JmsTemplate(connectionFactory);

		if (jmsProperties.isJacksonMessageConverterEnabled())
		{
			jmsTemplate.setMessageConverter(jacksonMessageConverter());
		}

		return jmsTemplate;
	}

	private MessageConverter jacksonMessageConverter()
	{
		var objectMapper = new ObjectMapper()
				.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.enableDefaultTyping(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, JsonTypeInfo.As.PROPERTY)
				.registerModule(new JavaTimeModule());

		var converter = new MappingJackson2MessageConverter();
		converter.setObjectMapper(objectMapper);
		converter.setTargetType(MessageType.TEXT);
		converter.setTypeIdPropertyName("jmsObjectTypeId");

		return converter;
	}
}
