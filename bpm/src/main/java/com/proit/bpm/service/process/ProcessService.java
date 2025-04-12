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

package com.proit.bpm.service.process;

import com.proit.bpm.domain.Process;
import com.proit.bpm.domain.Task;
import com.proit.bpm.domain.TaskResult;
import com.proit.bpm.exception.BpmException;
import com.proit.bpm.model.EngineType;
import com.proit.bpm.model.ProcessTimer;
import com.proit.bpm.model.TaskFilter;
import com.proit.bpm.repository.ProcessRepository;
import com.proit.bpm.service.task.TaskManager;
import com.proit.bpm.service.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcessService
{
	private final ProcessRepository processRepository;

	private final WorkflowService workflowService;
	private final TaskManager taskManager;
	private final List<ProcessEngine> processEngines;

	private Map<EngineType, ProcessEngine> processEnginesByType;

	@PostConstruct
	public void initialize()
	{
		processEnginesByType = processEngines.stream().collect(Collectors.toMap(ProcessEngine::getEngineType, Function.identity()));
	}

	public Process startProcess(String workflowId)
	{
		return startProcess(workflowId, Map.of());
	}

	public Process startProcess(String workflowId, Map<String, Object> parameters)
	{
		return getProcessEngineByWorkflow(workflowId).startProcess(workflowService.getActualWorkflowId(workflowId), parameters);
	}

	public Optional<Process> startProcessOnEvent(String event, Map<String, Object> eventParameters)
	{
		return startProcessOnEvent(event, eventParameters, Map.of());
	}

	public Optional<Process> startProcessOnEvent(String event, Map<String, Object> eventParameters, Map<String, Object> processParameters)
	{
		return processEngines.stream()
				.map(engine -> engine.startProcessOnEvent(event, eventParameters, processParameters))
				.filter(Optional::isPresent).findFirst()
				.orElse(Optional.empty());
	}

	public void stopProcess(String processId)
	{
		getProcessEngineByProcess(processId).stopProcess(processId);
	}

	public void deleteProcess(String processId)
	{
		getProcessEngineByProcess(processId).deleteProcess(processId);
	}

	public void killProcess(String processId)
	{
		getProcessEngineByProcess(processId).killProcess(processId);
	}

	public void migrateProcess(String processId, String workflowId)
	{
		getProcessEngineByProcess(processId).migrateProcess(processId, workflowService.getActualWorkflowId(workflowId));
	}

	void updateProcessParameter(String processId, String parameterName, Object parameterValue)
	{
		updateProcessParameters(processId, Map.of(parameterName, parameterValue));
	}

	void updateProcessParameters(String processId, Map<String, Object> parameters)
	{
		getProcessEngineByProcess(processId).updateProcessParameters(processId, parameters);
	}

	public List<ProcessTimer> getProcessTimers(String processId)
	{
		return getProcessEngineByProcess(processId).getProcessTimers(processId);
	}

	public void updateProcessTimer(String processId, String timerName, LocalDateTime nextFireTime)
	{
		getProcessEngineByProcess(processId).updateProcessTimer(processId, timerName, nextFireTime);
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
		sendEvent(process.getId(), event, eventParameters);
	}

	public void sendEvent(String processId, String event, Map<String, Object> eventParameters)
	{
		getProcessEngineByProcess(processId).sendEvent(processId, event, eventParameters);
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
		getProcessEngineByTask(taskId).completeTask(taskId, new TaskResult(actionId, actionParameters));
	}

	public void cancelTask(String taskId)
	{
		getProcessEngineByTask(taskId).cancelTask(taskId);
	}

	private ProcessEngine getProcessEngineByWorkflow(String workflowId)
	{
		return getProcessEngine(workflowService.getWorkflow(workflowId).getEngineType());
	}

	private ProcessEngine getProcessEngineByTask(String taskId)
	{
		var task = taskManager.getTaskRepository().findByIdEx(taskId);

		return getProcessEngineByProcess(task.getProcessId());
	}

	private ProcessEngine getProcessEngineByProcess(String processId)
	{
		var workflowId = processRepository.findByIdEx(processId).getWorkflowId();
		var engineType = workflowService.getWorkflow(workflowId).getEngineType();

		return getProcessEngine(engineType);
	}

	private ProcessEngine getProcessEngine(EngineType engineType)
	{
		return Optional.of(processEnginesByType.get(engineType)).orElseThrow(() -> new RuntimeException("engine type '%s' is not supported".formatted(engineType)));
	}
}
