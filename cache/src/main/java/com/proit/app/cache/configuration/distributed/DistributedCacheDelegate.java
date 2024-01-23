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

package com.proit.app.cache.configuration.distributed;

import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

public class DistributedCacheDelegate implements Cache
{
	private final DistributedCacheService distributedCacheService;

	private final Cache delegate;

	private final String cacheName;

	public DistributedCacheDelegate(Cache cache, DistributedCacheService distributedCacheService)
	{
		this.distributedCacheService = distributedCacheService;
		this.delegate = cache;

		cacheName = cache.getName();
	}

	@Override
	public String getName()
	{
		return delegate.getName();
	}

	@Override
	public Object getNativeCache()
	{
		return delegate.getNativeCache();
	}

	@Override
	public ValueWrapper get(Object key)
	{
		return delegate.get(key);
	}

	@Override
	public <T> T get(Object key, Class<T> type)
	{
		return delegate.get(key, type);
	}

	@Override
	public <T> T get(Object key, Callable<T> valueLoader)
	{
		return delegate.get(key, valueLoader);
	}

	@Override
	public void put(Object key, Object value)
	{
		distributedCacheService.put(cacheName, key, value);

		delegate.put(key, value);
	}

	@Override
	public void evict(Object key)
	{
		distributedCacheService.evict(cacheName, key);

		delegate.evict(key);
	}

	@Override
	public void clear()
	{
		distributedCacheService.invalidate(cacheName);

		delegate.clear();
	}
}
