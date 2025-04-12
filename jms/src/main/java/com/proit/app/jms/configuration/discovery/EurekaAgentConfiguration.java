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

package com.proit.app.jms.configuration.discovery;

import com.netflix.appinfo.ApplicationInfoManager;
import com.proit.app.jms.configuration.conditional.ConditionalOnEmbeddedBroker;
import com.proit.app.jms.configuration.properties.JmsProperties;
import lombok.RequiredArgsConstructor;
import org.apache.activemq.transport.discovery.DiscoveryAgent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnEmbeddedBroker
@ConditionalOnClass(name = {"com.netflix.appinfo.ApplicationInfoManager"})
@RequiredArgsConstructor
public class EurekaAgentConfiguration
{
	private final JmsProperties jmsProperties;

	@Bean
	public DiscoveryAgent eurekaDiscoveryAgent(ApplicationInfoManager applicationInfoManager, DiscoveryClient discoveryClient)
	{
		return new EurekaDiscoveryAgent(applicationInfoManager, discoveryClient, jmsProperties.getPort());
	}
}
