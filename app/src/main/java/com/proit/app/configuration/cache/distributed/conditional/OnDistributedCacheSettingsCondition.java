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

package com.proit.app.configuration.cache.distributed.conditional;

import com.proit.app.configuration.properties.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class OnDistributedCacheSettingsCondition extends SpringBootCondition
{
	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata)
	{
		if (context.getEnvironment() instanceof ConfigurableEnvironment env)
		{
			for (PropertySource<?> propertySource : env.getPropertySources())
			{
				if (propertySource instanceof EnumerablePropertySource)
				{
					for (String key : ((EnumerablePropertySource) propertySource).getPropertyNames())
					{
						if (key.startsWith(CacheProperties.DISTRIBUTED_OPERATIONS_ACTIVATION_PROPERTY))
						{
							return ConditionOutcome.match();
						}
					}
				}
			}
		}

		return ConditionOutcome.noMatch("no distributed cache settings detected");
	}
}
