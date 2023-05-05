/*
 *    Copyright 2019-2023 the original author or authors.
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

package com.proit.app.configuration.jms;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.netflix.appinfo.ApplicationInfoManager;
import com.proit.app.configuration.jms.discovery.EurekaDiscoveryAgent;
import com.proit.app.configuration.jms.discovery.NoOpDiscoveryAgent;
import com.proit.app.configuration.properties.JmsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.policy.*;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.network.DiscoveryNetworkConnector;
import org.apache.activemq.plugin.StatisticsBrokerPlugin;
import org.apache.activemq.store.PersistenceAdapter;
import org.apache.activemq.store.kahadb.KahaDBPersistenceAdapter;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;
import org.apache.activemq.transport.discovery.DiscoveryAgent;
import org.apache.activemq.util.DefaultIOExceptionHandler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
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
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

import javax.jms.ConnectionFactory;
import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Configuration
@EnableJms
@EnableConfigurationProperties(JmsProperties.class)
@ConditionalOnProperty(JmsProperties.ACTIVATION_PROPERTY)
@RequiredArgsConstructor
public class JmsConfiguration
{
	public static final String DLQ_PREFIX = "DLQ.";
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
	public BrokerService brokerService(DiscoveryAgent discoveryAgent, List<JmsBrokerChannelProvider> channelProviders) throws Exception
	{
		if (jmsProperties.isForceReleaseLock())
		{
			var lockFile = new File(jmsProperties.getStorePath(), "lock");

			if (lockFile.exists())
			{
				log.info("Force releasing JMS store lock file: {}.", lockFile);

				if (!lockFile.delete())
				{
					throw new RuntimeException("Failed to delete JMS store lock file!");
				}
			}
		}

		if (StringUtils.isBlank(jmsProperties.getBrokerName()))
		{
			jmsProperties.setBrokerName(String.format("app-instance-%s", UUID.randomUUID()));

			log.warn("Jms broker name is not specified. Using auto generated name: {}.", jmsProperties.getBrokerName());
		}

		var ioExceptionHandler = new DefaultIOExceptionHandler();
		ioExceptionHandler.setIgnoreNoSpaceErrors(false);
		ioExceptionHandler.setIgnoreSQLExceptions(false);

		BrokerService brokerService = new BrokerService();
		brokerService.addConnector(getConnectorURI());
		brokerService.setDestinationPolicy(setupDestinationPolicies());
		brokerService.setPersistent(true);
		brokerService.setPersistenceAdapter(persistenceAdapter());
		brokerService.setBrokerName(jmsProperties.getBrokerName());
		brokerService.setPlugins(new BrokerPlugin[] {
				new StatisticsBrokerPlugin()
		});
		brokerService.setIoExceptionHandler(ioExceptionHandler);

		if (jmsProperties.getStoreSizeLimit() != null)
		{
			brokerService.getSystemUsage().getStoreUsage().setLimit(DataSize.parse(jmsProperties.getStoreSizeLimit()).toBytes());

			log.info("JMS store size limit: {}.", jmsProperties.getStoreSizeLimit());
		}

		setupNetworkConnector(brokerService, channelProviders, discoveryAgent);

		try
		{
			brokerService.start();
		}
		catch (Exception e)
		{
			var storeIndex = new File(jmsProperties.getStorePath(), "db.data");

			if (storeIndex.exists())
			{
				log.error("Failed to start broker. Will try to rebuild JMS store index when app starts again.", e);

				if (!storeIndex.delete())
				{
					log.error("Failed to delete JMS store index file!");
				}
			}

			throw e;
		}

		return brokerService;
	}

	@Bean
	@Primary
	public ConnectionFactory jmsConnectionFactory(BrokerService broker) throws Exception
	{
		CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(createConnectionFactory());
		cachingConnectionFactory.setSessionCacheSize(10);
		cachingConnectionFactory.setCacheConsumers(false);

		return cachingConnectionFactory;
	}

	@Bean
	public ConnectionFactory nonCachingJmsConnectionFactory(BrokerService broker) throws Exception
	{
		return createConnectionFactory();
	}

	private ConnectionFactory createConnectionFactory() throws Exception
	{
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(getConnectorURI());
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

	private URI getConnectorURI() throws Exception
	{
		return new URI("tcp://" + jmsProperties.getHost() + ":" + jmsProperties.getPort().toString());
	}

	private PolicyMap setupDestinationPolicies()
	{
		var policyEntry = new PolicyEntry();
		policyEntry.setQueue(">");
		policyEntry.setDeadLetterStrategy(buildDeadLetterStrategy(0));

		PolicyMap destinationPolicy = new PolicyMap();
		destinationPolicy.setDefaultEntry(policyEntry);

		jmsProperties.getChannels().forEach((channelName, channelPolicy) -> {
			var entry = new PolicyEntry();

			var channelDeadLetterPolicy = channelPolicy.getDeadLetterPolicy();

			if (channelDeadLetterPolicy != null)
			{
				if (!channelDeadLetterPolicy.isEnabled())
				{
					entry.setDeadLetterStrategy(new DiscardingDeadLetterStrategy());
				}
				else
				{
					entry.setDeadLetterStrategy(buildDeadLetterStrategy(channelDeadLetterPolicy.getExpiration()));
				}
			}
			else
			{
				entry.setDeadLetterStrategy(buildDeadLetterStrategy(0));
			}

			Optional.ofNullable(channelPolicy.getMemoryLimit()).ifPresent(value -> entry.setMemoryLimit(DataSize.parse(value, DataUnit.BYTES).toBytes()));
			Optional.ofNullable(channelPolicy.getCursorMemoryHighWaterMark()).ifPresent(entry::setCursorMemoryHighWaterMark);
			Optional.ofNullable(channelPolicy.getStoreUsageHighWaterMark()).ifPresent(entry::setStoreUsageHighWaterMark);

			if (channelPolicy.getChannelType() == JmsProperties.ChannelType.TOPIC)
			{
				entry.setTopic(channelName);

				Optional.ofNullable(channelPolicy.getPrefetch()).ifPresent(entry::setTopicPrefetch);

				destinationPolicy.put(new ActiveMQTopic(channelName), entry);
			}
			else
			{
				entry.setQueue(channelName);

				Optional.ofNullable(channelPolicy.getPrefetch()).ifPresent(entry::setQueuePrefetch);

				destinationPolicy.put(new ActiveMQQueue(channelName), entry);
			}
		});

		return destinationPolicy;
	}

	private DeadLetterStrategy buildDeadLetterStrategy(long expiration)
	{
		var strategy = new IndividualDeadLetterStrategy();
		strategy.setQueuePrefix(DLQ_PREFIX);
		strategy.setTopicPrefix(DLQ_PREFIX);
		strategy.setExpiration(expiration);

		return strategy;
	}

	private PersistenceAdapter persistenceAdapter()
	{
		if (jmsProperties.getStoreType() == JmsProperties.StoreType.PERSISTENT)
		{
			KahaDBPersistenceAdapter adapter = new KahaDBPersistenceAdapter();
			adapter.setDirectory(new File(jmsProperties.getStorePath()));
			adapter.setCheckForCorruptJournalFiles(true);
			adapter.setChecksumJournalFiles(true);
			adapter.setIgnoreMissingJournalfiles(true);

			return adapter;
		}
		else
		{
			return new MemoryPersistenceAdapter();
		}
	}

	private void setupNetworkConnector(BrokerService brokerService, List<JmsBrokerChannelProvider> channelProviders, DiscoveryAgent discoveryAgent) throws Exception
	{
		var brokerQueues = new HashSet<>(jmsProperties.getBrokerChannels().getQueues());
		var brokerTopics = new HashSet<>(jmsProperties.getBrokerChannels().getTopics());

		channelProviders.forEach(channelProvider -> {
			brokerQueues.addAll(channelProvider.getQueues());
			brokerTopics.addAll(channelProvider.getTopics());
		});

		if (brokerQueues.isEmpty() && brokerTopics.isEmpty())
		{
			log.info("JMS broker has no external channels and won't be connected to broker network.");

			return;
		}

		var connector = new DiscoveryNetworkConnector();
		connector.setDuplex(jmsProperties.isDuplex());
		connector.setDiscoveryAgent(discoveryAgent);

		if (jmsProperties.getNetworkConnector() != null)
		{
			connector.setUri(new URI(jmsProperties.getNetworkConnector()));
		}

		brokerQueues.forEach(channel -> connector.addDynamicallyIncludedDestination(new ActiveMQQueue(channel)));
		brokerTopics.forEach(channel -> connector.addDynamicallyIncludedDestination(new ActiveMQTopic(channel)));

		brokerService.addNetworkConnector(connector);
	}

	// TODO: избавиться от @Primary.
	@Configuration
	@ConditionalOnProperty(JmsProperties.ACTIVATION_PROPERTY)
	@ConditionalOnClass(name = {"com.netflix.appinfo.ApplicationInfoManager"})
	@RequiredArgsConstructor
	public static class EurekaAgentConfiguration
	{
		private final JmsProperties jmsProperties;

		@Bean
		@Primary
		public DiscoveryAgent eurekaDiscoveryAgent(ApplicationInfoManager applicationInfoManager, DiscoveryClient discoveryClient)
		{
			return new EurekaDiscoveryAgent(applicationInfoManager, discoveryClient, jmsProperties.getPort());
		}
	}

	@Bean
	@ConditionalOnMissingBean
	public DiscoveryAgent noOpDiscoveryAgent()
	{
		return new NoOpDiscoveryAgent();
	}
}
