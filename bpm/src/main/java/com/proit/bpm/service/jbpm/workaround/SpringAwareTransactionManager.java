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

package com.proit.bpm.service.jbpm.workaround;

import org.drools.persistence.api.TransactionManager;
import org.drools.persistence.api.TransactionSynchronization;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SpringAwareTransactionManager implements TransactionManager
{
	private final ThreadLocal<AtomicInteger> suspendedTransactionCount = ThreadLocal.withInitial(AtomicInteger::new);
	private final ThreadLocal<Map<Object, Object>> transactionResources = ThreadLocal.withInitial(HashMap::new);
	private final ThreadLocal<List<TransactionSynchronization>> transactionSynchronizations = ThreadLocal.withInitial(ArrayList::new);

	private final ThreadLocal<Boolean> synchronizedWithSpring = ThreadLocal.withInitial(() -> Boolean.FALSE);

	@Override
	public int getStatus()
	{
		return TransactionManager.STATUS_ACTIVE;
	}

	@Override
	public boolean begin()
	{
		registerSpringTransactionSynchronization();

		return true;
	}

	@Override
	public void commit(boolean transactionOwner)
	{
	}

	@Override
	public void rollback(boolean transactionOwner)
	{
	}

	@Override
	public void registerTransactionSynchronization(TransactionSynchronization ts)
	{
		transactionSynchronizations.get().add(ts);
	}

	@Override
	public void putResource(Object key, Object resource)
	{
		transactionResources.get().put(key, resource);
	}

	@Override
	public Object getResource(Object key)
	{
		return transactionResources.get().get(key);
	}

	private void registerSpringTransactionSynchronization()
	{
		if (!synchronizedWithSpring.get())
		{

			TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
				@Override
				public int getOrder()
				{
					return Ordered.HIGHEST_PRECEDENCE;
				}

				@Override
				public void suspend()
				{
					var transactionNumber = suspendedTransactionCount.get().incrementAndGet();

					TransactionSynchronizationManager.bindResource(this + "_resources_" + transactionNumber, transactionResources.get());
					TransactionSynchronizationManager.bindResource(this + "_synchronizations_" + transactionNumber, transactionSynchronizations.get());

					transactionResources.remove();
					transactionSynchronizations.remove();
					synchronizedWithSpring.remove();
				}

				@Override
				@SuppressWarnings("unchecked")
				public void resume()
				{
					var transactionNumber = suspendedTransactionCount.get().getAndDecrement();

					transactionResources.set((Map<Object, Object>) TransactionSynchronizationManager.unbindResource(this + "_resources_" + transactionNumber));
					transactionSynchronizations.set((List<TransactionSynchronization>) TransactionSynchronizationManager.unbindResource(this + "_synchronizations_" + transactionNumber));
					synchronizedWithSpring.set(Boolean.TRUE);
				}

				@Override
				public void beforeCompletion()
				{
					for (TransactionSynchronization synchronization : transactionSynchronizations.get())
					{
						synchronization.beforeCompletion();
					}
				}

				@Override
				public void afterCompletion(int status)
				{
					for (TransactionSynchronization synchronization : transactionSynchronizations.get())
					{
						synchronization.afterCompletion(status);
					}

					transactionResources.remove();
					transactionSynchronizations.remove();
					synchronizedWithSpring.remove();
				}
			});

			synchronizedWithSpring.set(Boolean.TRUE);
		}
	}
}
