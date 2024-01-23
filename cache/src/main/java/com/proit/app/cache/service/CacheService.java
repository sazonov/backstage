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

package com.proit.app.cache.service;

import com.proit.app.cache.configuration.properties.CacheProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = CacheProperties.ACTIVATION_PROPERTY, matchIfMissing = true)
public class CacheService
{
	private final CacheManager cacheManager;

	public Set<String> getCacheNames()
	{
		return Set.copyOf(cacheManager.getCacheNames());
	}

	public void invalidate()
	{
		getCacheNames().forEach(this::invalidate);
	}

	public void invalidate(String cacheName)
	{
		Optional.ofNullable(cacheManager.getCache(cacheName)).ifPresent(Cache::clear);
	}

	public void invalidate(String cacheName, Object key)
	{
		Optional.ofNullable(cacheManager.getCache(cacheName)).ifPresent(cache -> cache.evict(key));
	}

	public void invalidate(String cacheName, Collection<Object> keys)
	{
		keys.forEach(key -> invalidate(cacheName, key));
	}
}
