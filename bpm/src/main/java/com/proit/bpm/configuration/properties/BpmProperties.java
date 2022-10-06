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

package com.proit.bpm.configuration.properties;

import com.proit.bpm.service.workflow.ClasspathWorkflowProvider;
import com.proit.bpm.service.workflow.DatabaseWorkflowProvider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

@Getter
@Setter
@ConfigurationProperties("app.bpm")
public class BpmProperties
{
	@Getter
	@RequiredArgsConstructor
	public enum WorkflowProviderType
	{
		CLASSPATH(ClasspathWorkflowProvider.class),
		DATABASE(DatabaseWorkflowProvider.class);

		private final Class<?> providerClass;

		public static WorkflowProviderType byProviderClass(Class<?> providerClass)
		{
			return Arrays.stream(values()).filter(it -> it.providerClass.isAssignableFrom(providerClass)).findFirst().orElse(null);
		}
	}

	/**
	 * Позволяет отключить деплой процессов на выбранных инстансах приложения.
	 */
	private boolean deployEnabled = true;

	/**
	 * Определяет источники, из которых будут загружаться описания бизнес-процессов.
	 */
	private Set<WorkflowProviderType> workflowProviders = EnumSet.allOf(WorkflowProviderType.class);

	/**
	 * Если параметр установлен, то все создаваемые таймеры будут срабатывать через указанное в нём количество секунд.
	 */
	private int overriddenTimerDelay = 0;

	/**
	 * Максимальный размер кэша скриптов процессов.
	 */
	private int scriptCacheSize = 1000;

	/**
	 * Включает автоматическое обновление запущенных процессов до последних ревизий.
	 */
	private boolean autoMigrateProcesses = false;

	/**
	 * Определяет задержку, после которой будут удалены неактивные (остановленные) процессы.
	 */
	private Duration deleteInactiveProcessesDelay;

	/**
	 * Активирует пессимистичные блокировки при обновлении процессов/задач. Не требуется при наличии одного инстанса приложения.
	 */
	private boolean pessimisticLocking = false;
}
