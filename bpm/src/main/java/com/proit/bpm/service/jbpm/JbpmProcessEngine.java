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

/*
 * Интеллектуальная собственность ООО "Про Ай-Ти Ресурс".
 * Использование ограничено прилагаемой лицензией.
 */

package com.proit.bpm.service.jbpm;

import com.proit.bpm.configuration.properties.BpmProperties;
import com.proit.bpm.domain.Process;
import com.proit.bpm.domain.Task;
import com.proit.bpm.domain.TaskResult;
import com.proit.bpm.exception.BpmException;
import com.proit.bpm.model.ProcessTimer;
import com.proit.bpm.repository.ProcessRepository;
import com.proit.bpm.repository.TaskRepository;
import com.proit.bpm.repository.jbpm.ProcessInstanceInfoRepository;
import com.proit.bpm.repository.jbpm.SessionInfoRepository;
import com.proit.bpm.repository.jbpm.WorkItemInfoRepository;
import com.proit.bpm.service.ProcessEngine;
import com.proit.bpm.service.ProcessTimerService;
import com.proit.bpm.service.TaskManager;
import com.proit.bpm.service.jbpm.script.ScriptingExtension;
import com.proit.bpm.service.jbpm.timer.NoOpProcessTimerService;
import com.proit.bpm.service.jbpm.timer.QuartzTimerService;
import com.proit.bpm.service.jbpm.workaround.CustomSessionConfiguration;
import com.proit.bpm.service.jbpm.workaround.DisposeCommand;
import com.proit.bpm.service.jbpm.workaround.JpaProcessPersistenceContextManager;
import com.proit.bpm.service.jbpm.workaround.SpringAwareTransactionManager;
import com.proit.bpm.service.script.ScriptEngine;
import com.proit.bpm.service.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.drools.core.SessionConfiguration;
import org.drools.core.command.runtime.process.SetProcessInstanceVariablesCommand;
import org.drools.core.impl.EnvironmentFactory;
import org.jbpm.process.core.timer.TimerServiceRegistry;
import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.persistence.jpa.JPAKnowledgeService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class JbpmProcessEngine implements ApplicationContextAware, ProcessEngine
{
	public static final String EVENT_TIMER_TRIGGERED = "timerTriggered";
	public static final String EVENT_PARAM_TIMER_INSTANCE = "timerInstance";

	private static final String ENV_NAME_BUSINESS_CALENDAR = "jbpm.business.calendar";

	private static final String PROCESS_HANDLER_METHOD_START = "onProcessStart";
	private static final String PROCESS_HANDLER_METHOD_UPDATE = "onProcessUpdate";
	private static final String PROCESS_HANDLER_METHOD_STOP = "onProcessStop";
	private static final String PROCESS_HANDLER_METHOD_EVENT = "onProcessEvent";

	private final EntityManager em;

	private final TaskRepository taskRepository;
	private final ProcessRepository processRepository;

	private final SessionInfoRepository sessionInfoRepository;
	private final WorkItemInfoRepository workItemInfoRepository;
	private final ProcessInstanceInfoRepository processInstanceInfoRepository;

	private final TaskManager taskManager;
	private final WorkflowService workflowService;
	private final ScriptEngine scriptEngine;
	private final Map<String, ScriptingExtension> scriptingExtensions;

	private final NamedParameterJdbcTemplate jdbcTemplate;

	private final KnowledgeService knowledgeService;
	private final SpringAwareTransactionManager transactionManager;

	private final BpmProperties bpmProperties;

	private ProcessTimerService processTimerService;
	private ProcessCalendar processCalendar;

	private final Properties customSessionProperties = new Properties();

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
	{
		try
		{
			processCalendar = applicationContext.getBean(ProcessCalendar.class);

			log.info("Using business calendar for process timers.");
		}
		catch (NoSuchBeanDefinitionException ignore)
		{
		}

		try
		{
			var quartzTimerService = applicationContext.getBean(QuartzTimerService.class);

			processTimerService = quartzTimerService;

			log.info("Using Quartz for process timers.");

			TimerServiceRegistry.getInstance().registerTimerService("default", quartzTimerService);

			customSessionProperties.put("drools.timerService", "org.jbpm.process.core.timer.impl.RegisteredTimerServiceDelegate");

			if (bpmProperties.getOverriddenTimerDelay() > 0)
			{
				log.info("Using overridden timer delay: {} second(s).", bpmProperties.getOverriddenTimerDelay());
			}
		}
		catch (NoSuchBeanDefinitionException e)
		{
			processTimerService = new NoOpProcessTimerService();
		}
	}

	@Override
	public Process startProcess(String workflowId, Map<String, Object> parameters)
	{
		var process = new Process();
		process.setCreated(LocalDateTime.now());
		process.setWorkflowId(workflowId);
		process.setActive(true);
		process.getParameters().putAll(parameters);

		var session = loadSession(process);
		var processInstance = session.createProcessInstance(workflowId, parameters);

		process.setInstanceId(processInstance.getId());

		processRepository.saveAndFlush(process);

		ProcessUtils.setProcessId(processInstance, process.getId());

		executeProcessScript(process, PROCESS_HANDLER_METHOD_START);

		session.startProcessInstance(processInstance.getId());

		updateProcessState(process, session);

		return process;
	}

	@Override
	public Optional<Process> startProcessOnEvent(String event, Map<String, Object> eventParameters, Map<String, Object> processParameters)
	{
		var process = new Process();
		process.setCreated(LocalDateTime.now());
		process.setActive(true);
		process.getParameters().putAll(processParameters);

		var session = loadSession(process);

		session.addEventListener(new DefaultProcessEventListener() {
			@Override
			public void beforeProcessStarted(ProcessStartedEvent event)
			{
				if (process.getId() != null)
				{
					throw new BpmException("running more than one event based process at the same time is not supported");
				}

				process.setWorkflowId(event.getProcessInstance().getProcessId());
				process.setInstanceId(event.getProcessInstance().getId());

				processRepository.saveAndFlush(process);

				ProcessUtils.setProcessId(event.getProcessInstance(), process.getId());

				processParameters.forEach((key, value) -> ProcessUtils.setProcessVariable(event.getProcessInstance(), key, value));

				executeProcessScript(process, PROCESS_HANDLER_METHOD_START);
			}
		});

		session.signalEvent(event, eventParameters);

		if (process.getInstanceId() != null)
		{
			updateProcessState(process, session);

			return Optional.of(process);
		}

		return Optional.empty();
	}

	@Override
	public void stopProcess(String processId)
	{
		var process = processRepository.findByIdEx(processId);
		var session = loadSession(process);

		session.abortProcessInstance(process.getInstanceId());

		updateProcessState(process, session);
	}

	@Override
	public void killProcess(String processId)
	{
		var process = processRepository.findByIdEx(processId);
		var sessionId = process.getSessionId();
		var processInstanceId = process.getInstanceId();

		var workItemIds = taskRepository.findAllByProcessId(processId)
				.stream()
				.map(Task::getWorkItemId)
				.toList();

		sessionInfoRepository.deleteById(sessionId);
		workItemInfoRepository.deleteAllById(workItemIds);
		processInstanceInfoRepository.deleteById(processInstanceId);

		jdbcTemplate.update("""
				delete from jbpm_correlationpropertyinfo
				where correlationkey_id in (select id from jbpm_correlationkeyinfo where processinstanceid = :id)
				""", Map.of("id", processInstanceId));

		jdbcTemplate.update("""
				delete from jbpm_correlationkeyinfo where processinstanceid = :id
				""", Map.of("id", processInstanceId));

		taskRepository.deleteByProcessId(processId);
		processRepository.delete(process);
	}

	@Override
	public void deleteProcess(String processId)
	{
		var process = processRepository.findByIdEx(processId);
		var session = loadSession(process);

		taskRepository.deleteByProcessId(processId);
		processRepository.delete(process);

		session.destroy();
	}

	@Override
	public void completeTask(String taskId, TaskResult taskResult)
	{
		var task = taskRepository.findByIdEx(taskId);
		var process = task.getProcess();
		var session = loadSession(process);

		taskManager.completeTask(taskId, taskResult);

		var parameters = new HashMap<>(task.getResult().getParameters());
		parameters.put("action", task.getResult().getActionId());

		session.getWorkItemManager().completeWorkItem(task.getWorkItemId(), parameters);

		updateProcessState(task.getProcess(), session);
	}

	@Override
	public void cancelTask(String taskId)
	{
		var task = taskRepository.findByIdEx(taskId);
		var process = task.getProcess();
		var session = loadSession(process);

		taskManager.cancelTask(taskId);

		session.getWorkItemManager().abortWorkItem(task.getWorkItemId());

		updateProcessState(task.getProcess(), session);
	}

	@Override
	public void sendEvent(String processId, String event, Map<String, Object> eventParameters)
	{
		var process = processRepository.findByIdEx(processId);

		if (!process.isActive())
		{
			return;
		}

		var session = loadSession(process);

		if (EVENT_TIMER_TRIGGERED.equals(event))
		{
			session.signalEvent(event, eventParameters.get(EVENT_PARAM_TIMER_INSTANCE), process.getInstanceId());
		}
		else
		{
			executeProcessScript(process, PROCESS_HANDLER_METHOD_EVENT, event, eventParameters);

			session.signalEvent(event, eventParameters, process.getInstanceId());
		}

		updateProcessState(process, session);
	}

	@Override
	public void updateProcessParameters(String processId, Map<String, Object> parameters)
	{
		var processInstance = processRepository.findByIdEx(processId);
		var session = loadSession(processInstance);

		processInstance.getParameters().putAll(parameters);

		session.execute(new SetProcessInstanceVariablesCommand(processInstance.getInstanceId(), parameters));
	}

	@Override
	public void migrateProcess(String processId, String workflowId)
	{
		var processInstance = processRepository.findByIdEx(processId);
		var session = loadSession(processInstance);

		ProcessUtils.migrateProcess(processInstance, session.getProcessInstance(processInstance.getInstanceId()), workflowId);
	}

	@Override
	public List<ProcessTimer> getProcessTimers(String processId)
	{
		return processTimerService.getProcessTimers(processId);
	}

	@Override
	public void updateProcessTimer(String processId, String timerName, LocalDateTime nextFireTime)
	{
		processTimerService.updateProcessTimer(processId, timerName, nextFireTime);
	}

	private void updateProcessState(Process process, KieSession session)
	{
		executeProcessScript(process, PROCESS_HANDLER_METHOD_UPDATE);

		WorkflowProcessInstance processInstance = (WorkflowProcessInstance) session.getProcessInstance(process.getInstanceId());

		if (processInstance == null || processInstance.getState() == ProcessInstance.STATE_COMPLETED)
		{
			process.setActive(false);

			executeProcessScript(process, PROCESS_HANDLER_METHOD_STOP);
		}
		else
		{
			process.setActive(true);
		}
	}

	private KieSession loadSession(Process processInstance)
	{
		if (processRepository.getEntityManager().contains(processInstance))
		{
			processRepository.lock(processInstance, bpmProperties.isPessimisticLocking() ?
					LockModeType.PESSIMISTIC_WRITE : LockModeType.OPTIMISTIC_FORCE_INCREMENT);
		}

		Environment env = EnvironmentFactory.newEnvironment();

		env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, em.getEntityManagerFactory());
		env.set(EnvironmentName.CMD_SCOPED_ENTITY_MANAGER, em);
		env.set(EnvironmentName.APP_SCOPED_ENTITY_MANAGER, em);
		env.set(EnvironmentName.TRANSACTION_MANAGER, transactionManager);
		env.set(EnvironmentName.PERSISTENCE_CONTEXT_MANAGER, new JpaProcessPersistenceContextManager(env));
		env.set(ENV_NAME_BUSINESS_CALENDAR, processCalendar);

		KieSession session;

		if (processInstance.getSessionId() != null)
		{
			session = JPAKnowledgeService.loadStatefulKnowledgeSession(processInstance.getSessionId(), knowledgeService.getKieBase(), createSessionConfiguration(), env);
		}
		else
		{
			session = JPAKnowledgeService.newStatefulKnowledgeSession(knowledgeService.getKieBase(), createSessionConfiguration(), env);

			processInstance.setSessionId(session.getIdentifier());
		}

		scriptingExtensions.forEach(session::setGlobal);

		session.getWorkItemManager().registerWorkItemHandler("Human Task", new HumanTaskHandler(processInstance, taskManager));

		session.execute(new DisposeCommand());

		return session;
	}

	private SessionConfiguration createSessionConfiguration()
	{
		if (!customSessionProperties.isEmpty())
		{
			return new CustomSessionConfiguration(customSessionProperties);
		}
		else
		{
			return SessionConfiguration.newInstance();
		}
	}

	private void executeProcessScript(Process process, String method, Object ... args)
	{
		var script = workflowService.getWorkflowScript(process.getWorkflowId(), "ProcessHandler");

		if (script != null)
		{
			scriptEngine.execute(script, method, ArrayUtils.addAll(new Object[]{ process }, args));
		}
	}
}
