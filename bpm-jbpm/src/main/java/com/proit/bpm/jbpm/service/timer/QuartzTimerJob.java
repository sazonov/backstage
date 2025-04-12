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

package com.proit.bpm.jbpm.service.timer;

import com.proit.app.utils.TaskUtils;
import com.proit.bpm.exception.BpmException;
import com.proit.bpm.jbpm.service.JbpmProcessEngine;
import com.proit.bpm.repository.ProcessRepository;
import com.proit.bpm.service.process.ProcessEngine;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.drools.core.time.Trigger;
import org.jbpm.process.instance.timer.TimerInstance;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Map;

@Slf4j
@PersistJobDataAfterExecution
@RequiredArgsConstructor
public class QuartzTimerJob extends QuartzJobBean
{
	private final ProcessRepository processRepository;

	@Qualifier("jbpmProcessEngine")
	private final ProcessEngine processEngine;
	private final QuartzTimerService quartzTimerService;

	@Setter
	private String processId;

	@Setter
	private String timerId;

	@Setter
	private String timerName;

	@Setter
	private String triggerData;

	@Setter
	private String timerInstanceData;

	@Override
	protected void executeInternal(@NonNull JobExecutionContext context)
	{
		try
		{
			log.info("Firing timer. Id: '{}', name: '{}', process id: '{}'.", timerId, timerName, processId);

			var objectMapper = quartzTimerService.getObjectMapper();

			Trigger trigger = objectMapper.readerFor(Trigger.class).readValue(triggerData);
			TimerInstance timerInstance = objectMapper.readerFor(TimerInstance.class).readValue(timerInstanceData);

			var process = processRepository.findById(processId);

			if (process.isEmpty())
			{
				log.warn("Cannot find process '{}'.", processId);

				return;
			}

			if (trigger.hasNextFireTime() == null)
			{
				timerInstance.setPeriod(0);
			}

			TaskUtils.executeWithTryCount(3, 1000, () -> {
				processEngine.sendEvent(process.get().getId(), JbpmProcessEngine.EVENT_TIMER_TRIGGERED, Map.of(JbpmProcessEngine.EVENT_PARAM_TIMER_INSTANCE, timerInstance));
			});

			if (trigger.hasNextFireTime() != null)
			{
				var nextFireTime = trigger.nextFireTime();

				var newJobTrigger = TriggerBuilder.newTrigger()
						.startAt(nextFireTime)
						.build();

				var jobDataMap = context.getJobDetail().getJobDataMap();
				jobDataMap.put("triggerData", objectMapper.writerFor(Trigger.class).writeValueAsString(trigger));
				jobDataMap.put("timerInstanceData", objectMapper.writerFor(TimerInstance.class).writeValueAsString(timerInstance));

				context.getScheduler().rescheduleJob(context.getTrigger().getKey(), newJobTrigger);

				log.info("Timer rescheduled. Id: '{}', name: '{}', fire time: '{}', process id: '{}'.", timerId, timerName, nextFireTime, processId);
			}
		}
		catch (Exception e)
		{
			throw new BpmException("failed to execute process timer", e);
		}
	}
}
