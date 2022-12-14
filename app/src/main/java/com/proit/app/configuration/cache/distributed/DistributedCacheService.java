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

package com.proit.app.configuration.cache.distributed;

import com.proit.app.configuration.cache.distributed.conditional.ConditionalOnDistributedCacheSettings;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnDistributedCacheSettings
@RequiredArgsConstructor
public class DistributedCacheService
{
	@Resource(name = "cacheJmsTemplate")
	private JmsTemplate jmsTemplate;

	@Getter
	private String nodeId;

	@PostConstruct
	public void initialize()
	{
		nodeId = UUID.randomUUID().toString();

		log.info("Initializing distributed cache node '{}'.", nodeId);
	}

	public void invalidate(String cacheName)
	{
		evict(cacheName, null);
	}

	public void evict(String cacheName, Object key)
	{
		put(cacheName, key, null);
	}

	public void put(String cacheName, Object key, Object value)
	{
		var currentItem = DistributedCacheItemSynchronizer.getCurrentItem().get();

		// Исключаем возникновение бесконечной петли.
		if (currentItem == null || !StringUtils.equals(cacheName, currentItem.getCacheName()) || !Objects.equals(key, currentItem.getKey()))
		{
			jmsTemplate.convertAndSend(DistributedCacheConfiguration.DISTRIBUTED_CACHE_ITEMS_TOPIC, new DistributedCacheItem(cacheName, key, value, nodeId));
		}
	}
}
