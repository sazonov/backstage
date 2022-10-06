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

package com.proit.app.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class TransactionConfiguration
{
	@Primary
	@Bean("transactionManager")
	public PlatformTransactionManager chainedTransactionManager(List<PlatformTransactionManager> transactionManagers)
	{
		if (transactionManagers.isEmpty())
		{
			return null;
		}

		if (transactionManagers.size() == 1)
		{
			return transactionManagers.get(0);
		}

		List<PlatformTransactionManager> orderedManagers = new ArrayList<>(transactionManagers.size());
		PlatformTransactionManager jpaTransactionManager = null;

		for (var manager : transactionManagers)
		{
			if (manager instanceof JpaTransactionManager)
			{
				jpaTransactionManager = manager;
			}
			else
			{
				orderedManagers.add(manager);
			}
		}

		if (jpaTransactionManager != null)
		{
			orderedManagers.add(jpaTransactionManager);
		}

		return new ChainedTransactionManager(orderedManagers.toArray(new PlatformTransactionManager[0]));
	}

	@Bean
	public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager)
	{
		return new TransactionTemplate(transactionManager);
	}
}
