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

package com.proit.app.cache.configuration.ehcache;

import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

@SuppressWarnings("unchecked")
public class EnhancedTransactionAwareCacheDecorator extends TransactionAwareCacheDecorator
{
	private final ThreadLocal<Map<Object, Object>> localCache = ThreadLocal.withInitial(HashMap::new);

	public EnhancedTransactionAwareCacheDecorator(Cache targetCache)
	{
		super(targetCache);
	}

	@Override
	public ValueWrapper get(Object key)
	{
		ValueWrapper result = super.get(key);

		if (result == null && TransactionSynchronizationManager.isSynchronizationActive())
		{
			Object localResult = getLocal(key);

			if (localResult != null)
			{
				return new SimpleValueWrapper(localResult);
			}
		}

		return result;
	}

	@Override
	public <T> T get(Object key, Class<T> type)
	{
		T result = super.get(key, type);

		if (result == null && TransactionSynchronizationManager.isSynchronizationActive())
		{
			return (T) getLocal(key);
		}

		return result;
	}

	@Override
	public <T> T get(Object key, Callable<T> valueLoader)
	{
		if (TransactionSynchronizationManager.isSynchronizationActive())
		{
			Object localResult = getLocal(key);

			if (localResult != null)
			{
				return (T) localResult;
			}
		}

		return super.get(key, valueLoader);
	}

	@Override
	public void put(Object key, Object value)
	{
		if (TransactionSynchronizationManager.isSynchronizationActive())
		{
			putLocal(key, value);

			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
				@Override
				public void afterCompletion(int status)
				{
					clearLocal();
				}
			});
		}

		super.put(key, value);
	}

	private Object getLocal(Object key)
	{
		return localCache.get().get(key);
	}

	private void putLocal(Object key, Object value)
	{
		localCache.get().put(key, value);
	}

	private void clearLocal()
	{
		localCache.remove();
	}
}
