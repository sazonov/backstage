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

package com.proit.app.jms.configuration.health;

import com.google.common.collect.ImmutableMap;
import com.proit.app.dict.service.health.AbstractHealthIndicator;
import com.proit.app.jms.configuration.conditional.ConditionalOnJms;
import org.apache.activemq.broker.BrokerService;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnJms
public class JmsStoreHealthIndicator extends AbstractHealthIndicator
{
	private final BrokerService brokerService;

	public JmsStoreHealthIndicator(BrokerService brokerService)
	{
		this.brokerService = brokerService;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder)
	{
		var storeUsage = brokerService.getSystemUsage().getStoreUsage();
		long actualDirectorySize = FileUtils.sizeOfDirectory(brokerService.getSystemUsage()
				.getStoreUsage().getStore().getDirectory());

		var statistic = ImmutableMap.of("name", storeUsage.getName(),
				"started", storeUsage.isStarted(),
				"full", storeUsage.isFull(),
				"limit", storeUsage.getLimit(),
				"directorySize", actualDirectorySize,
				"percentUsage", storeUsage.getPercentUsage(),
				"percentLimit", storeUsage.getPercentLimit(),
				"usagePortion", storeUsage.getUsagePortion(),
				"size", storeUsage.getStore().size());

		builder.withDetails(statistic);

		if (storeUsage.isFull())
		{
			builder.down();
		}
		else
		{
			builder.up();
		}
	}
}
