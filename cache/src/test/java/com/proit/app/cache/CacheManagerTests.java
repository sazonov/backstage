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

package com.proit.app.cache;

import com.proit.app.utils.TimeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheManagerTests extends AbstractTests
{
	@Autowired private CacheManager cacheManager;

	@Test
	void singleton()
	{
		var cacheName = "singletonCache";
		var firstItemId = "1";
		var secondItemId = "2";

		var cache = cacheManager.getCache(cacheName);

		assertNotNull(cache);

		cache.put(firstItemId, new Object());
		cache.put(secondItemId, new Object());

		assertNull(cache.get(firstItemId));
		assertNotNull(cache.get(secondItemId));
	}

	@Test
	void ttl()
	{
		var cacheName = "ttlCache";
		var ttl = 1000;
		var itemId = "1";

		var cache = cacheManager.getCache(cacheName);

		assertNotNull(cache);

		cache.put(itemId, new Object());

		assertNotNull(cache.get(itemId));

		TimeUtils.sleep(ttl + 100);

		assertNull(cache.get(itemId));
	}

	@Test
	public void tti()
	{
		var cacheName = "ttiCache";
		var ttl = 1000;
		var tti = 3000;
		var itemId = "1";

		var cache = cacheManager.getCache(cacheName);

		assertNotNull(cache);

		cache.put(itemId, new Object());

		assertNotNull(cache.get(itemId));

		TimeUtils.sleep(ttl + tti / 5);

		assertNotNull(cache.get(itemId));

		TimeUtils.sleep(tti);

		assertNull(cache.get(itemId));
	}

	@Test
	void heap()
	{
		var cacheName = "objects";
		var itemId = "1";
		var item = new Object();

		var cache = cacheManager.getCache(cacheName);

		assertNotNull(cache);

		cache.put(itemId, item);

		assertNotNull(cache.get(itemId));
		assertEquals(cache.get(itemId).get(), item);
	}
}
