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

package com.proit.bpm.service.handler;

import com.proit.bpm.domain.Process;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Интерфейс обработчика событий бизнес-процесса. Позволяет реагировать на все этапы жизненного цикла
 * процесса @{@link com.proit.bpm.domain.Process}.
 */
public abstract class AbstractProcessHandler
{
	/**
	 * Вызывается перед запуском процесса.
	 */
	public void onProcessStart(@Nonnull Process process)
	{
	}

	/**
	 * Вызывается после обновления процесса.
	 */
	public void onProcessUpdate(@Nonnull Process process)
	{
	}

	/**
	 * Вызывается после завершения процесса.
	 */
	public void onProcessStop(@Nonnull Process process)
	{
	}

	/**
	 * Вызывается при наступлении события event.
	 */
	public void onProcessEvent(@Nonnull Process process, @Nonnull String event, @Nonnull Map<String, Object> eventParameters)
	{
	}
}
