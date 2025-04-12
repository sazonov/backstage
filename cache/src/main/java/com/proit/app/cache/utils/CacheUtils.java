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

package com.proit.app.cache.utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.proit.app.cache.utils.proxy.ReadOnlyObjectProxyFactory;
import com.proit.app.exception.ObjectsNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnClass(name = "org.springframework.cache.CacheManager")
public class CacheUtils implements ApplicationContextAware
{
	private static final int MAX_FETCH_SIZE = 300;

	private static final LoadingCache<String, Object> locks = CacheBuilder.newBuilder().build(CacheLoader.from(Object::new));

	private static CacheManager defaultCacheManager = null;

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
	{
		try
		{
			defaultCacheManager = applicationContext.getBean(CacheManager.class);
		}
		catch (Exception ignored)
		{
			// no handling
		}
	}

	public static <T, S> List<T> getCachedItems(Collection<S> objectIds, Function<Collection<S>, Collection<T>> objectSource, String cacheName, Function<T, S> cacheKeyGenerator, Class<T> targetCass)
	{
		return getCachedItems(objectIds, objectSource, defaultCacheManager, cacheName, cacheKeyGenerator, targetCass, true);
	}

	public static <T, S> List<T> getCachedItems(Collection<S> objectIds, Function<Collection<S>, Collection<T>> objectSource, String cacheName, Function<T, S> cacheKeyGenerator, Class<T> targetCass, boolean createProxy)
	{
		if (defaultCacheManager == null)
		{
			throw new RuntimeException("no default cache manager detected");
		}

		return getCachedItems(objectIds, objectSource, defaultCacheManager, cacheName, cacheKeyGenerator, targetCass, createProxy);
	}

	public static <T, S> List<T> getCachedItems(Collection<S> objectIds, Function<Collection<S>, Collection<T>> objectSource, CacheManager cacheManager, String cacheName, Function<T, S> cacheKeyGenerator, Class<T> targetCass, boolean createProxy)
	{
		if (objectIds.isEmpty())
		{
			return Collections.emptyList();
		}

		objectIds = new LinkedHashSet<>(objectIds);

		final Map<S, T> result = new HashMap<>();
		List<S> nonCachedItems = new ArrayList<>();

		final Cache cache = cacheManager.getCache(cacheName);

		if (cache == null)
		{
			throw new RuntimeException(String.format("cannot get cache '%s'", cacheName));
		}

		updateCachedItems(cache, objectIds, result, nonCachedItems, targetCass);

		if (!nonCachedItems.isEmpty())
		{
			try
			{
				synchronized (locks.get(cacheName))
				{
					// Обновляем второй раз, чтобы захватить результаты других потоков.
					updateCachedItems(cache, objectIds, result, nonCachedItems, targetCass);

					if (!nonCachedItems.isEmpty())
					{
						int currentIndex = 0;
						List<T> fetchedObjects = new ArrayList<>(nonCachedItems.size());

						while (currentIndex < nonCachedItems.size())
						{
							int lastIndex = Math.min(nonCachedItems.size(), currentIndex + MAX_FETCH_SIZE);

							fetchedObjects.addAll(objectSource.apply(nonCachedItems.subList(currentIndex, lastIndex)));

							currentIndex = lastIndex;
						}

						fetchedObjects.forEach(it -> {
							T uos = createProxy ? ReadOnlyObjectProxyFactory.createProxy(it, targetCass) : it;
							S key = cacheKeyGenerator.apply(it);

							log.debug("Caching item of type '{}' with id '{}'.", targetCass.getSimpleName(), key);

							result.put(key, uos);
							cache.put(key, uos);
						});
					}
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException("failed to fetch items for caching", e);
			}
		}

		if (result.size() < objectIds.size())
		{
			List<String> missingObjectIds = objectIds.stream().filter(it -> !result.containsKey(it)).map(Object::toString).collect(Collectors.toList());

			throw new ObjectsNotFoundException(targetCass, missingObjectIds);
		}

		return objectIds.stream().map(result::get).collect(Collectors.toList());
	}

	private static <T, S> void updateCachedItems(Cache cache, Collection<S> objectIds, Map<S, T> result, List<S> nonCachedItems, Class<T> targetCass)
	{
		objectIds = nonCachedItems.isEmpty() ? objectIds : new ArrayList<>(nonCachedItems);

		nonCachedItems.clear();

		for (S objectId : objectIds)
		{
			Cache.ValueWrapper wrapper = cache.get(objectId);

			if (wrapper != null)
			{
				result.put(objectId, targetCass.cast(wrapper.get()));
			}
			else
			{
				nonCachedItems.add(objectId);
			}
		}
	}
}