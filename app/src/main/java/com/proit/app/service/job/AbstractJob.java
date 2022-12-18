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

package com.proit.app.service.job;

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
public abstract class AbstractJob implements Runnable
{
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
	 * Весь код задачи должен быть реализован в этой процедуре.
	 * @return Результаты задачи, которые будут доступны в health индикаторе.
	 */
	protected abstract JobResult execute();
}
