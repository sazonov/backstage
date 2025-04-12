/*
 *    Copyright 2019-2024 the original author or authors.
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

package com.proit.bpm.jbpm.service.workaround.runtime;

import com.proit.bpm.jbpm.service.ProcessUtils;
import com.proit.bpm.jbpm.service.timer.QuartzTimerService;
import lombok.extern.slf4j.Slf4j;
import org.drools.core.common.InternalKnowledgeRuntime;
import org.drools.core.time.TimerService;
import org.jbpm.process.core.timer.impl.RegisteredTimerServiceDelegate;

@Slf4j
public class TimerManager extends org.jbpm.process.instance.timer.TimerManager
{
	private final InternalKnowledgeRuntime knowledgeRuntime;
	private final TimerService timerService;

	public TimerManager(InternalKnowledgeRuntime kruntime, TimerService originalTimerService)
	{
		super(kruntime, originalTimerService);

		knowledgeRuntime = kruntime;

		if (getTimerService() instanceof RegisteredTimerServiceDelegate timerServiceDelegate)
		{
			timerService = timerServiceDelegate.getTimerService();
		}
		else
		{
			timerService = getTimerService();
		}
	}

	@Override
	public void cancelTimer(long processInstanceId, long timerId)
	{
		if (timerService instanceof QuartzTimerService quartzTimerService)
		{
			ProcessUtils.getProcessId(knowledgeRuntime.getProcessInstance(processInstanceId)).ifPresent(processId -> {
				quartzTimerService.getTimerInstances(processId).forEach(timerInstance ->
					getTimerMap().put(timerInstance.getId(), timerInstance)
				);
			});

			if (!getTimerMap().containsKey(timerId))
			{
				log.warn("Cannot cancel timer! Quartz timer service has no timer instance {} for process instance {}.", timerId, processInstanceId);

				return;
			}
		}

		super.cancelTimer(processInstanceId, timerId);
	}
}
