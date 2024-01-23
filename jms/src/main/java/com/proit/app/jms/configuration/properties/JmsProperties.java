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

package com.proit.app.jms.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties("app.jms")
public class JmsProperties
{
	public static final String ACTIVATION_PROPERTY = "app.jms.enabled";

	private boolean enabled = false;

	public enum ChannelType
	{
		TOPIC,
		QUEUE
	}

	public enum StoreType
	{
		MEMORY,
		PERSISTENT
	}

	@Getter
	@Setter
	public static class ChannelPolicy
	{
		@Getter
		@Setter
		public static class DeadLetterPolicy
		{
			private boolean enabled;

			private long expiration;
		}

		private ChannelType channelType = ChannelType.TOPIC;

		private Integer tryCount = 6;

		private Integer retryInterval = 1000;

		/**
		 * Например: 100MB
		 */
		private String memoryLimit;

		private Integer prefetch;

		private Integer cursorMemoryHighWaterMark;

		private Integer storeUsageHighWaterMark;

		private DeadLetterPolicy deadLetterPolicy;
	}

	@Getter
	@Setter
	public static class BrokerChannels
	{
		private List<String> queues = new LinkedList<>();

		private List<String> topics = new LinkedList<>();
	}

	private String brokerName;

	private StoreType storeType = StoreType.PERSISTENT;

	private String storePath = "data/jms";

	/**
	 * Например: 100GB
	 */
	private String storeSizeLimit = "100GB";

	private String host = "0.0.0.0";

	private Integer port = 62001;

	private String networkConnector;

	private boolean duplex = false;

	/**
	 * Принудительное удаление lock файла в data папке при старте приложения.
	 */
	private boolean forceReleaseLock = true;

	private Map<String, ChannelPolicy> channels = new HashMap<>();

	private BrokerChannels brokerChannels = new BrokerChannels();

	private boolean jacksonMessageConverterEnabled = true;
}
