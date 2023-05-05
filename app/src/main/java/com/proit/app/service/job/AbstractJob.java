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

package com.proit.app.service.job;

import com.proit.app.model.dto.job.EmptyJobParams;
import com.proit.app.model.dto.job.JobParams;
import com.proit.app.model.other.JobResult;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.Trigger;

import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

@Getter
@Setter
@Slf4j
public abstract class AbstractJob<T extends JobParams> implements Runnable
{
	private T defaultParams = createDefaultParams();

	private JobEventListener eventListener;

	protected Trigger trigger;

	private ScheduledFuture<?> future;

	public void beforeScheduled()
	{
		Optional.ofNullable(getEventListener()).ifPresent(it -> it.beforeSchedule(this));
	}

	public void afterScheduled()
	{
		Optional.ofNullable(getEventListener()).ifPresent(it -> it.afterSchedule(this));
	}

	public void cancel()
	{
		Optional.ofNullable(getEventListener()).ifPresent(it -> it.onCancel(this));

		if (future != null)
		{
			future.cancel(false);
		}
	}

	public final void run()
	{
		Optional.ofNullable(getEventListener()).ifPresent(it -> it.beforeExecute(this));

		try
		{
			var result = execute();

			Optional.ofNullable(getEventListener()).ifPresent(it -> it.afterExecute(this, result));
		}
		catch (Exception e)
		{
			log.error("Failed to execute job.", e);

			Optional.ofNullable(getEventListener()).ifPresent(it -> it.onException(this, e));
		}
	}

	/**
	 * Весь код задачи должен быть реализован в этой процедуре,
	 * если не предполагается использование параметров.
	 * В ином случае будет вызван {@link AbstractJob#execute(T)} c параметрами,
	 * определенными в {@link AbstractJob#createDefaultParams()}
	 */
	protected JobResult execute()
	{
		return execute(defaultParams);
	}

	/**
	 * Весь код задачи должен быть реализован в этой процедуре.
	 * @param params объект изменяемых параметров для задачи,
	 *                  должен быть наследником {@link JobParams}
	 *
	 * @return Результаты задачи, которые будут доступны в health индикаторе.
	 */
	protected JobResult execute(T params)
	{
		return JobResult.failed();
	}

	/**
	 * Создает объект параметров по умолчанию.
	 * Необходимо переопределять для задач, использующих изменяемый набор параметров при запуске.
	 *
	 * @return объект параметров, который будет использован при обращении к {@link AbstractJob#execute()}
	 */
	@SuppressWarnings("unchecked")
	protected T createDefaultParams()
	{
		return (T) new EmptyJobParams();
	}
}
