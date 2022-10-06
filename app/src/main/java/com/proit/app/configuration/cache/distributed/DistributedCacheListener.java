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
import com.proit.app.configuration.properties.CacheProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.CacheManager;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Slf4j
@Service
@ConditionalOnDistributedCacheSettings
@RequiredArgsConstructor
public class DistributedCacheListener
{
	@Resource(name = "cacheManager")
	private CacheManager cacheManager;

	private final DistributedCacheService distributedCacheService;

	private final CacheProperties cacheProperties;

	private String nodeId;

	@PostConstruct
	public void initialize()
	{
		nodeId = distributedCacheService.getNodeId();
	}

	@JmsListener(destination = DistributedCacheConfiguration.DISTRIBUTED_CACHE_ITEMS_TOPIC, containerFactory = "cacheListenerContainerFactory")
	public void onItemReceived(DistributedCacheItem item)
	{
		try
		{
			DistributedCacheItemSynchronizer.setCurrentItem(item);

			// Обрабатываем сообщения только от других узлов.
			if (!StringUtils.equals(item.getSourceNode(), nodeId))
			{
				log.debug("Processing distributed cache item: {}.", item);

				var cacheSettings = cacheProperties.getDistributedOperations().get(item.getCacheName());

				if (cacheSettings != null)
				{
					var targetCache = cacheManager.getCache(item.getCacheName());

					if (targetCache != null)
					{
						if (item.getKey() == null)
						{
							targetCache.invalidate();
						}
						else
						{
							if (item.getValue() != null && cacheSettings.contains(CacheProperties.DistributedCacheOperation.ANY))
							{
								log.debug("Applying new cache item: {}.", item);

								targetCache.put(item.getKey(), item.getValue());
							}
							else
							{
								log.debug("Clearing cache item: {}.", item);

								targetCache.evict(item.getKey());
							}
						}
					}
				}
			}
		}
		finally
		{
			DistributedCacheItemSynchronizer.setCurrentItem(null);
		}
	}
}
