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

import org.apache.activemq.command.DiscoveryEvent;
import org.apache.activemq.transport.discovery.DiscoveryAgent;
import org.apache.activemq.transport.discovery.DiscoveryListener;

import java.io.IOException;

public class NoOpDiscoveryAgent implements DiscoveryAgent
{
	@Override
	public void setDiscoveryListener(DiscoveryListener listener)
	{
	}

	@Override
	public void registerService(String name) throws IOException
	{
	}

	@Override
	public void serviceFailed(DiscoveryEvent event) throws IOException
	{
	}

	@Override
	public void start() throws Exception
	{
	}

	@Override
	public void stop() throws Exception
	{
	}
}
