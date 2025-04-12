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

package com.proit.app.database.utils;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.persistence.sessions.Session;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;

@Slf4j
@Component
public class JPAUtils implements ApplicationContextAware
{
	private static EntityManager em;

	public void setApplicationContext(ApplicationContext applicationContext)
	{
		try
		{
			em = applicationContext.getBean(EntityManager.class);
		}
		catch (Exception ignore)
		{
			log.warn("Failed to get entity manager while initializing JPAUtils.");
		}
	}

	public static Long getNextSequenceNumber(Class<?> domainObject)
	{
		if (em == null)
		{
			throw new RuntimeException("entity manager is not available");
		}

		return em.unwrap(Session.class).getNextSequenceNumberValue(domainObject).longValue();
	}
}
