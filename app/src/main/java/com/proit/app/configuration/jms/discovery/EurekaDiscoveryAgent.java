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

package com.proit.app.configuration.jms.discovery;

import com.netflix.appinfo.ApplicationInfoManager;
import com.proit.app.utils.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.DiscoveryEvent;
import org.apache.activemq.transport.discovery.DiscoveryAgent;
import org.apache.activemq.transport.discovery.DiscoveryListener;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class EurekaDiscoveryAgent implements DiscoveryAgent
{
	private static final String META_BROKER_PORT = "AMQ_BROKER_PORT";

	private final ApplicationInfoManager applicationInfoManager;
	private final DiscoveryClient discoveryClient;
	private final Integer brokerPort;

	@Setter
	private DiscoveryListener discoveryListener;

	private Thread discoveryThread;
	private boolean discoveryThreadActive;

	private final Set<String> discoveredConnectors = new HashSet<>();

	@Override
	public void registerService(String name)
	{
		synchronized (discoveredConnectors)
		{
			log.warn("ActiveMQ registered new service: {}.", name);

			discoveredConnectors.add(name);
		}
	}

	@Override
	public void serviceFailed(DiscoveryEvent event)
	{
		synchronized (discoveredConnectors)
		{
			log.warn("ActiveMQ cannot connect to service: {}.", event.getServiceName());

			discoveredConnectors.remove(event.getServiceName());
		}

		discoveryListener.onServiceRemove(event);
	}

	@Override
	public void start()
	{
		applicationInfoManager.registerAppMetadata(Map.of(
				META_BROKER_PORT, brokerPort.toString()
		));

		discoveryThread = new Thread(this::updateConnectors);
		discoveryThreadActive = true;
		discoveryThread.start();
	}

	@Override
	public void stop() throws Exception
	{
		if (discoveryThread != null)
		{
			discoveryThreadActive = false;
			discoveryThread.wait();
		}
	}

	private void updateConnectors()
	{
		while (discoveryThreadActive)
		{
			var connectors = new HashSet<String>();

			discoveryClient.getServices().forEach(serviceId -> {
				discoveryClient.getInstances(serviceId).forEach(service -> {
					if (!service.getInstanceId().equals(applicationInfoManager.getInfo().getId()))
					{
						var brokerPort = service.getMetadata().get(META_BROKER_PORT);

						if (brokerPort != null)
						{
							connectors.add("tcp://" + service.getHost() + ":" + brokerPort);
						}
					}
				});
			});

			synchronized (discoveredConnectors)
			{
				if (discoveredConnectors.size() != connectors.size())
				{
					log.info("Discovered ActiveMQ connectors changed: {}.", connectors);
				}

				discoveredConnectors.stream().filter(it -> !connectors.contains(it)).forEach(it -> discoveryListener.onServiceRemove(new DiscoveryEvent(it)));

				var newConnectors = connectors.stream().filter(it -> !discoveredConnectors.contains(it)).collect(Collectors.toSet());

				discoveredConnectors.clear();
				discoveredConnectors.addAll(connectors);

				newConnectors.forEach(connector -> discoveryListener.onServiceAdd(new DiscoveryEvent(connector)));
			}

			TimeUtils.sleepSeconds(20);
		}
	}
}
