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

import org.drools.persistence.api.PersistenceContext;
import org.drools.persistence.api.TransactionManager;
import org.drools.persistence.jpa.JpaPersistenceContext;
import org.drools.persistence.jpa.JpaPersistenceContextManager;
import org.jbpm.persistence.JpaProcessPersistenceContext;
import org.jbpm.persistence.api.ProcessPersistenceContext;
import org.jbpm.persistence.api.ProcessPersistenceContextManager;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;

import javax.persistence.EntityManager;

public final class JpaProcessPersistenceContextManager extends JpaPersistenceContextManager implements ProcessPersistenceContextManager
{
	private final Environment env;

	public JpaProcessPersistenceContextManager(Environment env)
	{
		super(env);

		this.env = env;
	}

	@Override
	public EntityManager getApplicationScopedEntityManager()
	{
		return (EntityManager) env.get(EnvironmentName.APP_SCOPED_ENTITY_MANAGER);
	}

	@Override
	public EntityManager getCommandScopedEntityManager()
	{
		return (EntityManager) env.get(EnvironmentName.CMD_SCOPED_ENTITY_MANAGER);
	}

	@Override
	public PersistenceContext getApplicationScopedPersistenceContext()
	{
		return new JpaPersistenceContext(getApplicationScopedEntityManager(), false, (TransactionManager) env.get(EnvironmentName.TRANSACTION_MANAGER));
	}

	@Override
	public PersistenceContext getCommandScopedPersistenceContext()
	{
		return new JpaPersistenceContext(getCommandScopedEntityManager(), false, (TransactionManager) env.get(EnvironmentName.TRANSACTION_MANAGER));
	}

	public ProcessPersistenceContext getProcessPersistenceContext()
	{
		return new JpaProcessPersistenceContext(getCommandScopedEntityManager(), false, false, null, (TransactionManager) env.get(EnvironmentName.TRANSACTION_MANAGER));
	}
}
