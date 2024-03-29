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

package com.proit.app.jobs.service;

import com.proit.app.jobs.model.dto.JobTrigger;
import com.proit.app.jobs.model.dto.JobTriggerType;
import com.proit.app.jobs.model.dto.param.JobParams;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * Абстрактный класс для задач, которые запускаются через фиксированный период времени.
 */
public abstract class AbstractFixedDelayJob<T extends JobParams> extends AbstractJob<T>
{
	public AbstractFixedDelayJob()
	{
		trigger = new PeriodicTrigger(getFixedDelay());
	}

	public abstract long getFixedDelay();

	@Override
	public JobTrigger getJobTrigger()
	{
		var trigger = (PeriodicTrigger) this.trigger;

		return JobTrigger.of(JobTriggerType.INTERVAL, String.valueOf(trigger.getPeriod()));
	}
}
