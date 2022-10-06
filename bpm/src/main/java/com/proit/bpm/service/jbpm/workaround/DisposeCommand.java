/*
 *    Copyright 2019-2022 the original author or authors.
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

/*
 * Интеллектуальная собственность ООО "Про Ай-Ти Ресурс".
 * Использование ограничено прилагаемой лицензией.
 */

package com.proit.bpm.service.jbpm.workaround;

import org.kie.api.command.ExecutableCommand;
import org.kie.api.runtime.Context;
import org.kie.api.runtime.KieSession;
import org.kie.internal.command.RegistryContext;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class DisposeCommand implements ExecutableCommand<Object>
{
	private boolean suspended;

	public Object execute(Context context)
	{
		final KieSession ksession = ((RegistryContext) context).lookup(KieSession.class);

		try
		{
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
			{
				@Override
				public void suspend()
				{
					suspended = true;
				}

				@Override
				public void resume()
				{
					suspended = false;
				}

				@Override
				public void afterCompletion(int status)
				{
					if (!suspended)
					{
						ksession.dispose();
					}
				}
			});
		}
		catch (Exception e)
		{
			throw new RuntimeException("failed to register transaction synchronization", e);
		}

		return null;
	}
}