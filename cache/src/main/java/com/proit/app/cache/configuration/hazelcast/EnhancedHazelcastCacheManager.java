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

package com.proit.app.cache.configuration.hazelcast;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.spring.cache.HazelcastCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.springframework.util.Assert.isTrue;

public class EnhancedHazelcastCacheManager implements CacheManager
{
	/**
	 * Property name for hazelcast spring-cache related properties.
	 */
	public static final String CACHE_PROP = "hazelcast.spring.cache.prop";

	private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<String, Cache>();

	private HazelcastInstance hazelcastInstance;

	/**
	 * Default cache value retrieval timeout. Apply to all caches.
	 * Can be overridden setting a cache specific value to readTimeoutMap.
	 */
	private long defaultReadTimeout;

	/**
	 * Holds cache specific value retrieval timeouts. Override defaultReadTimeout for specified caches.
	 */
	private Map<String, Long> readTimeoutMap = new HashMap<String, Long>();

	private boolean transactionAware;

	public EnhancedHazelcastCacheManager()
	{
	}

	public EnhancedHazelcastCacheManager(HazelcastInstance hazelcastInstance)
	{
		this.hazelcastInstance = hazelcastInstance;
	}

	public Cache getCache(String name)
	{
		Cache cache = caches.get(name);
		if (cache == null)
		{
			IMap<Object, Object> map = hazelcastInstance.getMap(name);
			cache = new HazelcastCache(map);

			long cacheTimeout = calculateCacheReadTimeout(name);
			((HazelcastCache) cache).setReadTimeout(cacheTimeout);

			Cache currentCache = caches.putIfAbsent(name, decorateCache(cache));

			if (currentCache != null)
			{
				cache = currentCache;
			}
		}

		return cache;
	}

	public void setTransactionAware(boolean transactionAware)
	{
		this.transactionAware = transactionAware;
	}

	public boolean isTransactionAware()
	{
		return this.transactionAware;
	}

	protected Cache decorateCache(Cache cache)
	{
		return (isTransactionAware() ? new TransactionAwareCacheDecorator(cache) : cache);
	}

	public Collection<String> getCacheNames()
	{
		Set<String> cacheNames = new HashSet<String>();
		Collection<DistributedObject> distributedObjects = hazelcastInstance.getDistributedObjects();

		for (DistributedObject distributedObject : distributedObjects)
		{
			if (distributedObject instanceof IMap)
			{
				IMap<?, ?> map = (IMap) distributedObject;
				cacheNames.add(map.getName());
			}
		}

		return cacheNames;
	}

	private long calculateCacheReadTimeout(String name)
	{
		Long timeout = getReadTimeoutMap().get(name);

		return timeout == null ? defaultReadTimeout : timeout;
	}

	public HazelcastInstance getHazelcastInstance()
	{
		return hazelcastInstance;
	}

	public void setHazelcastInstance(HazelcastInstance hazelcastInstance)
	{
		this.hazelcastInstance = hazelcastInstance;
	}

	private void parseOptions(String options)
	{
		if (null == options || options.trim().isEmpty())
		{
			return;
		}

		for (String option : options.split(","))
		{
			parseOption(option);
		}
	}

	private void parseOption(String option)
	{
		String[] keyValue = option.split("=");
		isTrue(keyValue.length != 0, "blank key-value pair");
		isTrue(keyValue.length <= 2, String.format("key-value pair %s with more than one equals sign", option));

		String key = keyValue[0].trim();
		String value = (keyValue.length == 1) ? null : keyValue[1].trim();

		isTrue(value != null && !value.isEmpty(), String.format("value for %s should not be null or empty", key));

		if ("defaultReadTimeout".equals(key))
		{
			defaultReadTimeout = Long.parseLong(value);
		}
		else
		{
			readTimeoutMap.put(key, Long.parseLong(value));
		}
	}

	/**
	 * Return default cache value retrieval timeout in milliseconds.
	 */
	public long getDefaultReadTimeout()
	{
		return defaultReadTimeout;
	}

	/**
	 * Set default cache value retrieval timeout. Applies to all caches, if not defined a cache specific timeout.
	 *
	 * @param defaultReadTimeout default cache retrieval timeout in milliseconds. 0 or negative values disable timeout.
	 */
	public void setDefaultReadTimeout(long defaultReadTimeout)
	{
		this.defaultReadTimeout = defaultReadTimeout;
	}

	/**
	 * Return cache-specific value retrieval timeouts. Map keys are cache names, values are cache retrieval timeouts in
	 * milliseconds.
	 */
	public Map<String, Long> getReadTimeoutMap()
	{
		return readTimeoutMap;
	}

	/**
	 * Set the cache ead-timeout params
	 *
	 * @param options cache read-timeout params, auto-wired by Spring automatically by getting value of
	 *                hazelcast.spring.cache.prop parameter
	 */
	@Autowired
	public void setCacheOptions(@Value("${" + CACHE_PROP + ":}") String options)
	{
		parseOptions(options);
	}
}
