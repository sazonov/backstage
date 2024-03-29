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

import com.proit.app.jobs.model.dto.other.JobResult;
import com.proit.app.dict.service.health.SimpleHealthIndicatorComponent;
import lombok.Getter;
import org.springframework.scheduling.support.SimpleTriggerContext;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JobHealthIndicator extends SimpleHealthIndicatorComponent implements JobEventListener
{
	private Date lastExecutionTime;
	private Date lastCompletionTime;

	@Getter
	private Date nextExecutionTime;

	public JobHealthIndicator(AbstractJob<?> job)
	{
		job.setEventListener(this);
	}

	@Override
	public void beforeSchedule(AbstractJob<?> job)
	{

	}

	@Override
	public void afterSchedule(AbstractJob<?> job)
	{
		nextExecutionTime = job.getTrigger().nextExecutionTime(new SimpleTriggerContext());

		upWithDetails(buildDetails());
	}

	@Override
	public void beforeExecute(AbstractJob<?> job)
	{
		lastExecutionTime = new Date();
	}

	@Override
	public void afterExecute(AbstractJob<?> job, JobResult jobResult)
	{
		lastCompletionTime = new Date();
		nextExecutionTime = job.getTrigger().nextExecutionTime(new SimpleTriggerContext(
				lastExecutionTime,
				lastExecutionTime,
				lastCompletionTime));

		if (jobResult.isSuccess())
		{
			upWithDetails(buildDetails(jobResult.getProperties()));
		}
		else
		{
			downWithDetails(buildDetails(jobResult.getProperties()));
		}
	}

	@Override
	public void onCancel(AbstractJob<?> job)
	{
	}

	@Override
	public void onException(AbstractJob<?> job, Exception exception)
	{
		lastCompletionTime = new Date();
		nextExecutionTime = job.getTrigger().nextExecutionTime(new SimpleTriggerContext(
				lastExecutionTime,
				lastExecutionTime,
				lastCompletionTime));

		downWithDetails(exception, buildDetails());
	}

	private Map<String, Object> buildDetails()
	{
		return buildDetails(Map.of());
	}

	private Map<String, Object> buildDetails(Map<String, Object> parameters)
	{
		var details = new HashMap<>(parameters);

		Optional.ofNullable(lastExecutionTime).ifPresent(it -> details.put("lastExecutionTime", lastExecutionTime));
		Optional.ofNullable(lastCompletionTime).ifPresent(it -> details.put("lastCompletionTime", lastCompletionTime));
		Optional.ofNullable(nextExecutionTime).ifPresent(it -> details.put("nextExecutionTime", nextExecutionTime));

		return details;
	}
}
