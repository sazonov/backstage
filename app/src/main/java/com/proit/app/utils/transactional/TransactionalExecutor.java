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

package com.proit.app.utils.transactional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Component
public class TransactionalExecutor
{
	/**
	 * Обработка действия внутри транзакции.
	 * @param action - действие
	 */
	@Transactional
	public void execute(Runnable action)
	{
		action.run();
	}

	/**
	 * Обработка действия внутри новой транзакции с получением результата.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public <T> T executeInNewTx(Supplier<T> supplier)
	{
		return supplier.get();
	}
}
