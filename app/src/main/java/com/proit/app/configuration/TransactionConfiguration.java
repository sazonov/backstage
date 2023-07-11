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

import com.proit.app.configuration.properties.TransactionProperties;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;

@Slf4j
@Configuration
@EnableConfigurationProperties(TransactionProperties.class)
@RequiredArgsConstructor
public class TransactionConfiguration implements ApplicationContextAware
{
	@Setter
	private ApplicationContext applicationContext;

	private final TransactionProperties transactionProperties;

	@ConditionalOnProperty(value = TransactionProperties.CHAIN_ACTIVATION_PROPERTY, matchIfMissing = true)
	public class ChainInitializer
	{
		@Primary
		@Bean("transactionManager")
		public PlatformTransactionManager chainedTransactionManager()
		{
			var transactionManagers = new ArrayList<PlatformTransactionManager>(transactionProperties.getChaining().size());

			for (String transactionManagerName : transactionProperties.getChaining())
			{
				try
				{
					transactionManagers.add(applicationContext.getBean(transactionManagerName, PlatformTransactionManager.class));
				}
				catch (Exception ignore)
				{
					log.warn("Failed to create a transaction manager chain. No transaction manager: {}.", transactionManagerName);
				}
			}

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
	}

	@Bean
	@ConditionalOnProperty(value = TransactionProperties.TEMPLATE_ACTIVATION_PROPERTY, matchIfMissing = true)
	public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager)
	{
		return new TransactionTemplate(transactionManager);
	}
}
