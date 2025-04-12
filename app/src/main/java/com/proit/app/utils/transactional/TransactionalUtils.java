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

package com.proit.app.utils.transactional;

import lombok.experimental.UtilityClass;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@UtilityClass
public class TransactionalUtils
{
	/**
	 * Обработка действия после коммита транзакции.
	 * @param action - действие
	 */
	public void doAfterCommit(Runnable action)
	{
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit()
			{
				action.run();
			}
		});
	}

	/**
	 * Обработка действия в случае корректного отката транзакции или ошибки, при которой коммит не возможен.
	 * @param action - действие
	 */
	public void doOnRollback(Runnable action)
	{
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status)
			{
				if (status != TransactionSynchronization.STATUS_COMMITTED)
				{
					action.run();
				}
			}
		});
	}
}
