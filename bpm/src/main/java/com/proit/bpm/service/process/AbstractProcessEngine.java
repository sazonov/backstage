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
import com.proit.bpm.model.ProcessContext;
import com.proit.bpm.model.Workflow;
import com.proit.bpm.repository.ProcessRepository;
import com.proit.bpm.repository.TaskRepository;
import com.proit.bpm.service.script.ScriptEngine;
import com.proit.bpm.service.task.TaskManager;
import com.proit.bpm.service.workflow.WorkflowService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Getter
@Transactional
@RequiredArgsConstructor
public abstract class AbstractProcessEngine<C extends ProcessContext> implements ProcessEngine
{
	public static final String UNDEFINED_INSTANCE_ID = "-1";
	public static final Long UNDEFINED_SESSION_ID = Long.parseLong(UNDEFINED_INSTANCE_ID);

	protected static final String PROCESS_HANDLER_SCRIPT = "ProcessHandler";
	protected static final String PROCESS_HANDLER_METHOD_START = "onProcessStart";
	protected static final String PROCESS_HANDLER_METHOD_UPDATE = "onProcessUpdate";
	protected static final String PROCESS_HANDLER_METHOD_STOP = "onProcessStop";
	protected static final String PROCESS_HANDLER_METHOD_EVENT = "onProcessEvent";

	private static final String TASK_PARAMETER_ACTION = "action";

	private final TaskRepository taskRepository;
	private final ProcessRepository processRepository;

	private final TaskManager taskManager;
	private final WorkflowService workflowService;
	private final ScriptEngine scriptEngine;

	private final EngineType engineType;

	protected Process createProcess(String workflowId, Map<String, Object> parameters)
	{
		var process = new Process();
		process.setEngineType(getEngineType());
		process.setCreated(LocalDateTime.now());
		process.setWorkflowId(workflowId);
		process.setActive(true);
		process.setSessionId(UNDEFINED_SESSION_ID);
		process.setInstanceId(UNDEFINED_INSTANCE_ID);
		process.getParameters().putAll(parameters);

		return getProcessRepository().saveAndFlush(process);
	}

	protected final C getProcessContext(String processId)
	{
		return getProcessContext(getProcessRepository().findByIdEx(processId));
	}

	protected abstract C getProcessContext(Process process);

	protected abstract boolean isProcessContextActive(C processContext);

	@Override
	public Process startProcess(String workflowId, Map<String, Object> parameters)
	{
		var process = createProcess(workflowId, parameters);
		var processContext = getProcessContext(process);

		executeProcessScript(process, PROCESS_HANDLER_METHOD_START);

		startProcessInternal(processContext);

		updateProcessState(processContext);

		return process;
	}

	protected abstract void startProcessInternal(C processContext);

	@Override
	public Optional<Process> startProcessOnEvent(String event, Map<String, Object> eventParameters, Map<String, Object> processParameters)
	{
		var process = createProcess(UNDEFINED_INSTANCE_ID, processParameters);
		var processContext = getProcessContext(process);

		startProcessOnEventInternal(processContext, event, eventParameters, () -> executeProcessScript(process, PROCESS_HANDLER_METHOD_START));

		if (UNDEFINED_INSTANCE_ID.equals(process.getInstanceId()))
		{
			deleteProcess(process.getId());

			return Optional.empty();
		}

		updateProcessState(processContext);

		return Optional.of(process);
	}

	protected abstract void startProcessOnEventInternal(C processContext, String event, Map<String, Object> eventParameters, Runnable beforeStartTrigger);

	@Override
	public void stopProcess(String processId)
	{
		var processContext = getProcessContext(processId);

		if (isProcessContextActive(processContext))
		{
			stopProcessInternal(processContext);

			updateProcessState(processContext);
		}
	}

	protected abstract void stopProcessInternal(C processContext);

	@Override
	public void deleteProcess(String processId)
	{
		var processContext = getProcessContext(processId);

		deleteProcessInternal(processContext);

		taskRepository.deleteByProcessId(processId);
		processRepository.delete(processContext.getProcess());
	}

	protected void deleteProcessInternal(C processContext)
	{
	}

	@Override
	public void completeTask(String taskId, TaskResult taskResult)
	{
		var task = getTaskRepository().findByIdEx(taskId);

		getTaskManager().completeTask(taskId, taskResult);

		var parameters = new HashMap<>(task.getResult().getParameters());
		parameters.put(TASK_PARAMETER_ACTION, task.getResult().getActionId());

		completeTaskInternal(task, parameters);

		updateProcessState(getProcessContext(task.getProcess()));
	}

	protected abstract void completeTaskInternal(Task task, Map<String, Object> parameters);

	@Override
	public void cancelTask(String taskId)
	{
		var task = getTaskRepository().findByIdEx(taskId);

		cancelTaskInternal(task);

		updateProcessState(getProcessContext(task.getProcess()));
	}

	protected abstract void cancelTaskInternal(Task task);

	@Override
	public void sendEvent(String processId, String event, Map<String, Object> eventParameters)
	{
		var processContext = getProcessContext(processId);

		if (!isProcessContextActive(processContext))
		{
			return;
		}

		executeProcessScript(processContext.getProcess(), PROCESS_HANDLER_METHOD_EVENT, event, eventParameters);

		sendEventInternal(processContext, event, eventParameters);

		updateProcessState(processContext);
	}

	protected abstract void sendEventInternal(C processContext, String event, Map<String, Object> eventParameters);

	@Override
	public void updateProcessParameters(String processId, Map<String, Object> parameters)
	{
		var processContext = getProcessContext(processId);

		processContext.getProcess().getParameters().putAll(parameters);

		updateProcessParametersInternal(processContext);
	}

	protected abstract void updateProcessParametersInternal(C processContext);

	@Override
	public void migrateProcess(String processId, String workflowId)
	{
		var context = getProcessContext(processId);

		var workflow = getWorkflowService().getWorkflow(context.getProcess().getWorkflowId());
		var targetWorkflow = getWorkflowService().getWorkflow(workflowId);

		log.info("Migrating process '{}'. Source workflow: '{}', target workflow: '{}'.", processId, workflow.getFullId(), targetWorkflow.getFullId());

		if (workflow.getEngineType() != targetWorkflow.getEngineType())
		{
			throw new BpmException("cannot migrate process '%s' because of engine type mismatch (source: '%s', target: '%s')".formatted(processId, workflow.getEngineType(), targetWorkflow.getEngineType()));
		}

		if (!workflow.getId().equals(targetWorkflow.getId()))
		{
			throw new BpmException("cannot migrate process to different workflow id (source: '%s', target: '%s')".formatted(workflow.getId(), targetWorkflow.getId()));
		}

		migrateWorkflowInternal(context, targetWorkflow);

		context.getProcess().setWorkflowId(targetWorkflow.getFullId());
	}

	protected abstract void migrateWorkflowInternal(C processContext, Workflow targetWorkflow);

	protected void updateProcessState(C processContext)
	{
		var process = processContext.getProcess();

		executeProcessScript(process, PROCESS_HANDLER_METHOD_UPDATE);

		if (!isProcessContextActive(processContext))
		{
			process.setActive(false);

			executeProcessScript(process, PROCESS_HANDLER_METHOD_STOP);
		}
		else
		{
			process.setActive(true);
		}

		processRepository.flush();
	}

	protected void executeProcessScript(Process process, String method, Object... args)
	{
		var script = getWorkflowService().getWorkflowScript(process.getWorkflowId(), PROCESS_HANDLER_SCRIPT);

		if (script != null)
		{
			getScriptEngine().execute(script, method, ArrayUtils.addAll(new Object[]{ process }, args));
		}
	}
}
