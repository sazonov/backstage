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

package com.proit.app.configuration.cache.distributed;

import com.proit.app.configuration.cache.distributed.conditional.ConditionalOnDistributedCacheSettings;
import com.proit.app.configuration.jms.JmsBrokerChannelProvider;
import com.proit.app.configuration.properties.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import java.util.Set;

@Configuration
@ConditionalOnProperty(value = CacheProperties.ACTIVATION_PROPERTY, matchIfMissing = true)
@ConditionalOnDistributedCacheSettings
public class DistributedCacheConfiguration
{
	public static final String DISTRIBUTED_CACHE_ITEMS_TOPIC = "distributedCacheItems";

	@Bean
	public JmsListenerContainerFactory<?> cacheListenerContainerFactory(ConnectionFactory connectionFactory)
	{
		DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setAutoStartup(true);
		factory.setSessionTransacted(false);
		factory.setPubSubDomain(true);
		factory.setSubscriptionDurable(false);
		factory.setConcurrency("1");
		factory.setErrorHandler(new DistributedCacheErrorHandler());

		return factory;
	}

	@Bean
	public JmsTemplate cacheJmsTemplate(ConnectionFactory connectionFactory)
	{
		var jmsTemplate = new JmsTemplate(connectionFactory);
		jmsTemplate.setPubSubDomain(true);
		jmsTemplate.setPubSubNoLocal(true);

		return jmsTemplate;
	}

	@Bean
	public JmsBrokerChannelProvider cacheJmsBrokerChannelProvider()
	{
		return new JmsBrokerChannelProvider() {
			@Override
			public Set<String> getTopics()
			{
				return Set.of(DISTRIBUTED_CACHE_ITEMS_TOPIC);
			}
		};
	}
}
