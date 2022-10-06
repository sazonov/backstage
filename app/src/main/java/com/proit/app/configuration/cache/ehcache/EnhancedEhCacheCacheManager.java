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

package com.proit.app.configuration.cache.ehcache;

import com.proit.app.configuration.cache.distributed.DistributedCacheDelegate;
import com.proit.app.configuration.cache.distributed.DistributedCacheService;
import com.proit.app.configuration.properties.CacheProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.ehcache.EhCacheCacheManager;

public class EnhancedEhCacheCacheManager extends EhCacheCacheManager
{
	private final DistributedCacheService distributedCacheService;

	private final CacheProperties cacheProperties;

	public EnhancedEhCacheCacheManager(DistributedCacheService distributedCacheService, CacheProperties cacheProperties)
	{
		this.distributedCacheService = distributedCacheService;
		this.cacheProperties = cacheProperties;

		setTransactionAware(cacheProperties.isTransactional());
	}

	@Override
	protected Cache decorateCache(Cache cache)
	{
		var result = isTransactionAware() ? new EnhancedTransactionAwareCacheDecorator(cache) : cache;

		return (distributedCacheService != null && cacheProperties.getDistributedOperations().containsKey(cache.getName()))
				? new DistributedCacheDelegate(result, distributedCacheService) : result;
	}
}
