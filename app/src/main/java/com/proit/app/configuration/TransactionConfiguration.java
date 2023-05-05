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

package com.proit.app.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Optional;

@Configuration
public class TransactionConfiguration
{
	@Primary
	@Bean("transactionManager")
	public PlatformTransactionManager chainedTransactionManager(Optional<JpaTransactionManager> jpaTransactionManager, Optional<JmsTransactionManager> jmsTransactionManager)
	{
		var transactionManagers = new ArrayList<PlatformTransactionManager>(2);

		jmsTransactionManager.ifPresent(transactionManagers::add);
		jpaTransactionManager.ifPresent(transactionManagers::add);

		if (transactionManagers.isEmpty())
		{
			return null;
		}
		else if (transactionManagers.size() == 1)
		{
			return transactionManagers.get(0);
		}

		return new ChainedTransactionManager(transactionManagers.toArray(new PlatformTransactionManager[0]));
	}

	@Bean
	public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager)
	{
		return new TransactionTemplate(transactionManager);
	}
}
