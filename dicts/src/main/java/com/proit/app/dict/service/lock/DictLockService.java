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

package com.proit.app.dict.service.lock;

import com.proit.app.dict.exception.dict.DictNotFoundException;
import com.proit.app.exception.AppException;
import com.proit.app.utils.functional.CallableEx;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class DictLockService
{
	/**
	 * Общая коллекция для хранения локов (ключом выступает идентификатор справочника).
	 * <p>При необходимости удалять элементы из коллекции необходимо рассмотреть добавление синхронизацию.</p>
	 */
	private final Map<String, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();

	/**
	 * Метод добавления лока в коллекцию, чтобы гарантировать наличие лока на каждый из представленных справочников.
	 * */
	public void addLock(String dictId)
	{
		Objects.requireNonNull(dictId, "dictId не может быть null.");

		locks.put(dictId, new ReentrantReadWriteLock());
	}

	public <R> R lockDictOperation(String dictId, CallableEx<R> callable)
	{
		Objects.requireNonNull(dictId, "dictId не может быть null.");

		var readerLock = Optional.ofNullable(locks.get(dictId))
				.map(ReentrantReadWriteLock::readLock)
				.orElseThrow(() -> new DictNotFoundException(dictId));

		return lockDictOperation(readerLock, callable);
	}

	public <R> R lockDictSchemaModifyOperation(String dictId, CallableEx<R> callable)
	{
		Objects.requireNonNull(dictId, "dictId не может быть null.");

		var writeLock = Optional.ofNullable(locks.get(dictId))
				.map(ReentrantReadWriteLock::writeLock)
				.orElseThrow(() -> new DictNotFoundException(dictId));

		return lockDictOperation(writeLock, callable);
	}

	private <R> R lockDictOperation(Lock lock, CallableEx<R> callable)
	{
		try
		{
			lock.lock();

			return callable.call();
		}
		catch (AppException e)
		{
			throw e;
		}
		catch (Throwable e)
		{
			throw new RuntimeException(e);
		}
		finally
		{
			lock.unlock();
		}
	}
}