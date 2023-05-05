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

package com.proit.bpm.service.jbpm.workaround;

import org.drools.core.SessionConfigurationImpl;
import org.drools.core.TimerJobFactoryType;
import org.kie.internal.utils.ChainedProperties;

import java.io.Serial;
import java.util.Properties;

/**
 * В комбинации с Quartz мы не используем JPA таймеры из jBPM, поэтому нет необходимости
 * устанавливать {@link TimerJobFactoryType.JPA}. Приличного способа управлять этим пока нет,
 * поэтому делаем отдельную реализацию для хранения конфигурации, которая всегда
 * возвращает {@link TimerJobFactoryType.THREAD_SAFE_TRACKABLE}.
 */
public class CustomSessionConfiguration extends SessionConfigurationImpl
{
	@Serial
	private static final long serialVersionUID = 510L;

	public CustomSessionConfiguration()
	{
		super();
	}

	public CustomSessionConfiguration(Properties properties)
	{
		super(properties);
	}

	public CustomSessionConfiguration(Properties properties, ClassLoader classLoader)
	{
		super(properties, classLoader);
	}

	public CustomSessionConfiguration(Properties properties, ClassLoader classLoader, ChainedProperties chainedProperties)
	{
		super(properties, classLoader, chainedProperties);
	}

	@Override
	public void setTimerJobFactoryType(TimerJobFactoryType timerJobFactoryType)
	{
	}

	@Override
	public TimerJobFactoryType getTimerJobFactoryType()
	{
		return TimerJobFactoryType.THREAD_SAFE_TRACKABLE;
	}
}
