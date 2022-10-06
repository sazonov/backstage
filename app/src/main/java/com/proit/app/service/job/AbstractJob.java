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

@Slf4j
public abstract class AbstractJob implements Runnable
{
	@Getter
	@Setter
	private JobEventListener eventListener;

	public void beforeScheduled()
	{
		Optional.ofNullable(getEventListener()).ifPresent(it -> it.beforeScheduled(this));
	}

	public void afterScheduled()
	{
		Optional.ofNullable(getEventListener()).ifPresent(it -> it.afterScheduled(this));
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

	public abstract Trigger getTrigger();

	/**
	 * Весь код задачи должен быть реализован в этой процедуре.
	 * @return Результаты задачи, которые будут доступны в health индикаторе.
	 */
	protected abstract JobResult execute();
}
