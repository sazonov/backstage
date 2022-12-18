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

package com.proit.bpm.service;

import com.proit.bpm.configuration.properties.BpmProperties;
import com.proit.bpm.domain.Process;
import com.proit.bpm.domain.Task;
import com.proit.bpm.domain.TaskResult;
import com.proit.bpm.exception.BpmException;
import com.proit.bpm.model.ProcessTimer;
import com.proit.bpm.model.TaskFilter;
import com.proit.bpm.repository.ProcessRepository;
import com.proit.bpm.service.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@EnableConfigurationProperties(BpmProperties.class)
@RequiredArgsConstructor
public class ProcessService
{
	private final ProcessRepository processRepository;

	private final WorkflowService workflowService;
	private final ProcessEngine processEngine;
	private final TaskManager taskManager;

	public Process startProcess(String workflowId)
	{
		return startProcess(workflowId, Map.of());
	}

	public Process startProcess(String workflowId, Map<String, Object> parameters)
	{
		return processEngine.startProcess(workflowService.getActualWorkflowId(workflowId), parameters);
	}

	public Optional<Process> startProcessOnEvent(String event, Map<String, Object> eventParameters)
	{
		return startProcessOnEvent(event, eventParameters, Map.of());
	}

	public Optional<Process> startProcessOnEvent(String event, Map<String, Object> eventParameters, Map<String, Object> processParameters)
	{
		return processEngine.startProcessOnEvent(event, eventParameters, processParameters);
	}

	public void stopProcess(String processId)
	{
		processEngine.stopProcess(processId);
	}

	public void deleteProcess(String processId)
	{
		processEngine.deleteProcess(processId);
	}

	public void killProcess(String processId)
	{
		processEngine.killProcess(processId);
	}

	public void migrateProcess(String processId, String workflowId)
	{
		processEngine.migrateProcess(processId, workflowService.getActualWorkflowId(workflowId));
	}

	void updateProcessParameter(String processId, String parameterName, Object parameterValue)
	{
		updateProcessParameters(processId, Map.of(parameterName, parameterValue));
	}

	void updateProcessParameters(String processId, Map<String, Object> parameters)
	{
		processEngine.updateProcessParameters(processId, parameters);
	}

	public List<ProcessTimer> getProcessTimers(String processId)
	{
		return processEngine.getProcessTimers(processId);
	}

	public void updateProcessTimer(String processId, String timerName, LocalDateTime nextFireTime)
	{
		processEngine.updateProcessTimer(processId, timerName, nextFireTime);
	}

	public void sendEvent(Process process, String event)
	{
		sendEvent(process.getId(), event);
	}

	public void sendEvent(String processId, String event)
	{
		sendEvent(processId, event, Collections.emptyMap());
	}

	public void sendEvent(Process process, String event, Map<String, Object> eventParameters)
	{
		processEngine.sendEvent(process.getId(), event, eventParameters);
	}

	public void sendEvent(String processId, String event, Map<String, Object> eventParameters)
	{
		processEngine.sendEvent(processId, event, eventParameters);
	}

	public Optional<Process> getProcess(String processId)
	{
		return processRepository.findById(processId);
	}

	public Optional<Process> getProcess(String keyName, String keyValue)
	{
		var processes = getProcesses(keyName, keyValue);

		if (processes.size() > 1)
		{
			throw new BpmException("key is not unique, more than one active process found");
		}
		else if (processes.size() == 1)
		{
			return Optional.of(processes.get(0));
		}
		else
		{
			return Optional.empty();
		}
	}

	public List<Process> getProcesses(String paramName1, String paramValue1)
	{
		return processRepository.findByParameters(true, paramName1, paramValue1);
	}

	public Optional<Task> getTask(TaskFilter taskFilter)
	{
		return taskManager.getTasks(taskFilter, PageRequest.of(0, 1)).stream().findFirst();
	}

	public Page<Task> getTasks(TaskFilter taskFilter, Pageable pageable)
	{
		return taskManager.getTasks(taskFilter, pageable);
	}

	public void assignTask(String taskId, String userId)
	{
		taskManager.assignTask(taskId, userId);
	}

	public void completeTask(Task task, String actionId)
	{
		completeTask(task.getId(), actionId, Map.of());
	}

	public void completeTask(String taskId, String actionId)
	{
		completeTask(taskId, actionId, Map.of());
	}

	public void completeTask(String taskId, String actionId, Map<String, Object> actionParameters)
	{
		processEngine.completeTask(taskId, new TaskResult(actionId, actionParameters));
	}

	public void cancelTask(String taskId)
	{
		processEngine.cancelTask(taskId);
	}
}
