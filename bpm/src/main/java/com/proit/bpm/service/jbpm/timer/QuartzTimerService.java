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

package com.proit.bpm.service.jbpm.timer;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.proit.bpm.configuration.properties.BpmProperties;
import com.proit.bpm.exception.BpmException;
import com.proit.bpm.model.ProcessTimer;
import com.proit.bpm.model.QuartzProcessTimer;
import com.proit.bpm.service.ProcessTimerService;
import com.proit.bpm.service.jbpm.ProcessUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.drools.core.time.*;
import org.drools.core.time.impl.TimerJobFactoryManager;
import org.drools.core.time.impl.TimerJobInstance;
import org.jbpm.process.instance.timer.TimerInstance;
import org.jbpm.process.instance.timer.TimerManager;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.jbpm.workflow.instance.node.StateBasedNodeInstance;
import org.jbpm.workflow.instance.node.TimerNodeInstance;
import org.quartz.JobBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@ConditionalOnClass(name = "org.quartz.Scheduler")
@RequiredArgsConstructor
public class QuartzTimerService implements TimerService, ProcessTimerService
{
	private final Scheduler scheduler;

	private final ProcessUtils processUtils;

	private final BpmProperties bpmProperties;

	@Getter
	private ObjectMapper objectMapper;

	private TimerJobFactoryManager timerJobFactoryManager;

	@PostConstruct
	public void initialize()
	{
		var typeValidator = BasicPolymorphicTypeValidator.builder()
				.allowIfBaseType(Trigger.class)
				.allowIfBaseType(TimerInstance.class)
				.build();

		objectMapper = JsonMapper.builder()
				.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.PROPERTY)
				.build();
	}

	public List<ProcessTimer> getProcessTimers(String processId)
	{
		try
		{
			List<ProcessTimer> result = new LinkedList<>();

			for (var jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(getTimerGroup(processId))))
			{
				var jobDetail = scheduler.getJobDetail(jobKey);
				var jobData = jobDetail.getJobDataMap();

				result.add(new QuartzProcessTimer(jobData.getString("timerName"), LocalDateTime.ofInstant(Instant.ofEpochMilli(jobData.getLong("fireTime")), ZoneId.systemDefault()), jobKey));
			}

			return result;
		}
		catch (SchedulerException e)
		{
			throw new BpmException("failed to get process timers", e);
		}
	}

	public void updateProcessTimer(String processId, String timerName, LocalDateTime nextFireTime)
	{
		var timer = (QuartzProcessTimer) getProcessTimers(processId).stream().filter(it -> it.getName().equals(timerName)).findFirst()
				.orElseThrow(() -> new BpmException(String.format("cannot find timer '%s'", timerName)));

		try
		{
			var triggers = scheduler.getTriggersOfJob(timer.getKey());
			var nextFireTimeDate = Date.from(nextFireTime.atZone(ZoneId.systemDefault()).toInstant());

			var newTrigger = TriggerBuilder.newTrigger()
				.startAt(nextFireTimeDate)
				.build();

			var job = scheduler.getJobDetail(timer.getKey());

			job.getJobDataMap().replace("fireTime", nextFireTimeDate.getTime());

			scheduler.rescheduleJob(triggers.get(0).getKey(), newTrigger);
			scheduler.addJob(job, true);

			log.info("Timer updated. Id: '{}', name: '{}', fire time: '{}', process id: '{}'.", timer.getKey().getName(), timerName, nextFireTimeDate, processId);
		}
		catch (SchedulerException e)
		{
			throw new BpmException("failed to update process timer", e);
		}
	}

	public long getCurrentTime()
	{
		return System.currentTimeMillis();
	}

	public void reset()
	{
	}

	public void shutdown()
	{
	}

	public long getTimeToNextJob()
	{
		return -1;
	}

	public Collection<TimerJobInstance> getTimerJobInstances(long id)
	{
		return Collections.emptyList();
	}

	public void setTimerJobFactoryManager(TimerJobFactoryManager timerJobFactoryManager)
	{
		this.timerJobFactoryManager = timerJobFactoryManager;
	}

	public TimerJobFactoryManager getTimerJobFactoryManager()
	{
		return timerJobFactoryManager;
	}

	public JobHandle scheduleJob(Job job, JobContext ctx, Trigger trigger)
	{
		try
		{
			if (!(ctx instanceof TimerManager.ProcessJobContext jobContext))
			{
				throw new BpmException("timer service supports only default job contexts");
			}

			var processInstance = (RuleFlowProcessInstance) jobContext.getWorkingMemory().getProcessInstance(jobContext.getProcessInstanceId());
			var processId = processUtils.getProcessId(processInstance);

			String timerName = null;

			for (var nodeInstance : processInstance.getNodeInstances(true))
			{
				if (nodeInstance instanceof TimerNodeInstance timerNodeInstance)
				{
					var timerId = timerNodeInstance.getTimerInstance() != null ? timerNodeInstance.getTimerInstance().getId() : timerNodeInstance.getTimerId();

					if (timerId == jobContext.getTimer().getId())
					{
						if (timerNodeInstance.getNodeName() != null)
						{
							timerName = timerNodeInstance.getNodeName() + "#" + timerId;
						}
					}
				}
				else if (nodeInstance instanceof StateBasedNodeInstance stateBasedNodeInstance)
				{
					var nodeTimerInstances = stateBasedNodeInstance.getTimerInstances();

					if (nodeTimerInstances != null && (nodeTimerInstances.isEmpty() || nodeTimerInstances.contains(jobContext.getTimer().getId())))
					{
						if (stateBasedNodeInstance.getNodeName() != null)
						{
							timerName = stateBasedNodeInstance.getNodeName() + "#" + jobContext.getTimer().getId();
						}
					}
				}
			}

			if (timerName == null)
			{
				timerName = "timer#" + jobContext.getTimer().getId();
			}

			String timerId = UUID.randomUUID().toString();
			String timerGroup = getTimerGroup(processId);

			Date nextFireTime = jobContext.getTrigger().nextFireTime();
			boolean isNextFireTimeOverridden = false;

			if (bpmProperties.getOverriddenTimerDelay() > 0)
			{
				nextFireTime = DateUtils.addSeconds(new Date(), bpmProperties.getOverriddenTimerDelay());
				isNextFireTimeOverridden = true;
			}

			var jobTrigger = TriggerBuilder.newTrigger()
					.startAt(nextFireTime)
					.build();

			var jobDetail = JobBuilder.newJob(QuartzTimerJob.class)
					.withIdentity(timerId, timerGroup)
					.usingJobData("processId", processId)
					.usingJobData("timerId", timerId)
					.usingJobData("timerName", timerName)
					.usingJobData("fireTime", nextFireTime.getTime())
					.usingJobData("triggerData", objectMapper.writerFor(Trigger.class).writeValueAsString(jobContext.getTrigger()))
					.usingJobData("timerInstanceData", objectMapper.writerFor(TimerInstance.class).writeValueAsString(jobContext.getTimer()))
					.build();

			scheduler.scheduleJob(jobDetail, jobTrigger);

			log.info("Timer created. Id: '{}', name: '{}', fire time: '{}'{}, process id: '{}'.", timerId, timerName, nextFireTime, isNextFireTimeOverridden ? " (overridden)" : "", processId);

			return new QuartzJobHandle(jobDetail.getKey());
		}
		catch (Exception e)
		{
			throw new BpmException("failed to create process timer", e);
		}
	}

	public boolean removeJob(JobHandle jobHandle)
	{
		if (!(jobHandle instanceof QuartzJobHandle quartzJobHandle))
		{
			throw new BpmException("timer service supports only quartz job handles");
		}

		try
		{
			scheduler.deleteJob(quartzJobHandle.getJobKey());
		}
		catch (SchedulerException e)
		{
			throw new BpmException("failed to remove timer", e);
		}

		return true;
	}

	private String getTimerGroup(String processId)
	{
		return "bpm_process_" + processId;
	}
}
