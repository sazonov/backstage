package com.proit.app.database.utils;

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
}
