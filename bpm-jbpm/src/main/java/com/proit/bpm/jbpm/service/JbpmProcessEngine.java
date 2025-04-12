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

package com.proit.bpm.jbpm.service;

import com.proit.bpm.configuration.properties.BpmProperties;
import com.proit.bpm.domain.Process;
import com.proit.bpm.domain.Task;
import com.proit.bpm.exception.BpmException;
import com.proit.bpm.jbpm.repository.ProcessInstanceInfoRepository;
import com.proit.bpm.jbpm.repository.SessionInfoRepository;
import com.proit.bpm.jbpm.repository.WorkItemInfoRepository;
import com.proit.bpm.jbpm.service.handler.DefaultProcessEventListener;
import com.proit.bpm.jbpm.service.handler.VariableChangeEventListener;
import com.proit.bpm.jbpm.service.timer.NoOpProcessTimerService;
import com.proit.bpm.jbpm.service.timer.ProcessTimerService;
import com.proit.bpm.jbpm.service.timer.QuartzTimerService;
import com.proit.bpm.jbpm.service.workaround.CustomSessionConfiguration;
import com.proit.bpm.jbpm.service.workaround.DisposeCommand;
import com.proit.bpm.jbpm.service.workaround.JpaProcessPersistenceContextManager;
import com.proit.bpm.jbpm.service.workaround.SpringAwareTransactionManager;
import com.proit.bpm.model.EngineType;
import com.proit.bpm.model.ProcessTimer;
import com.proit.bpm.model.Workflow;
import com.proit.bpm.repository.ProcessRepository;
import com.proit.bpm.repository.TaskRepository;
import com.proit.bpm.service.process.AbstractProcessEngine;
import com.proit.bpm.service.script.ScriptEngine;
import com.proit.bpm.service.script.extension.ScriptingExtensionLocator;
import com.proit.bpm.service.task.TaskManager;
import com.proit.bpm.service.workflow.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.drools.core.SessionConfiguration;
import org.drools.core.command.runtime.process.SetProcessInstanceVariablesCommand;
import org.drools.core.impl.EnvironmentFactory;
import org.jbpm.process.core.timer.TimerServiceRegistry;
import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.kie.api.event.process.ProcessStartedEvent;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

@Slf4j
@Service
@Transactional
public class JbpmProcessEngine extends AbstractProcessEngine<JbpmProcessContext> implements ApplicationContextAware
{
	public static final String EVENT_TIMER_TRIGGERED = "timerTriggered";
	public static final String EVENT_PARAM_TIMER_INSTANCE = "timerInstance";

	private static final String ENV_NAME_BUSINESS_CALENDAR = "jbpm.business.calendar";

	private final EntityManager em;

	private final SessionInfoRepository sessionInfoRepository;
	private final WorkItemInfoRepository workItemInfoRepository;
	private final ProcessInstanceInfoRepository processInstanceInfoRepository;

	private final ScriptingExtensionLocator scriptingExtensionLocator;

	private final NamedParameterJdbcTemplate jdbcTemplate;

	private final JbpmKnowledgeService knowledgeService;
	private final SpringAwareTransactionManager transactionManager;

	private ProcessCalendar processCalendar;
	private ProcessTimerService processTimerService;

	private final BpmProperties bpmProperties;
	private final Properties customSessionProperties = new Properties();

	public JbpmProcessEngine(TaskRepository taskRepository,
	                         ProcessRepository processRepository,
	                         SessionInfoRepository sessionInfoRepository,
	                         WorkItemInfoRepository workItemInfoRepository,
	                         ProcessInstanceInfoRepository processInstanceInfoRepository,
	                         TaskManager taskManager,
	                         WorkflowService workflowService,
	                         JbpmKnowledgeService knowledgeService,
	                         ScriptEngine scriptEngine,
	                         EntityManager em,
	                         ScriptingExtensionLocator scriptingExtensionLocator,
	                         NamedParameterJdbcTemplate jdbcTemplate,
	                         SpringAwareTransactionManager transactionManager,
	                         BpmProperties bpmProperties)
	{

		super(taskRepository, processRepository, taskManager, workflowService, scriptEngine, EngineType.JBPM);

		this.em = em;
		this.sessionInfoRepository = sessionInfoRepository;
		this.workItemInfoRepository = workItemInfoRepository;
		this.processInstanceInfoRepository = processInstanceInfoRepository;
		this.transactionManager = transactionManager;
		this.knowledgeService = knowledgeService;
		this.scriptingExtensionLocator = scriptingExtensionLocator;
		this.jdbcTemplate = jdbcTemplate;
		this.bpmProperties = bpmProperties;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
	{
		try
		{
			processCalendar = applicationContext.getBean(ProcessCalendar.class);

			log.info("Using business calendar for process timers.");
		}
		catch (NoSuchBeanDefinitionException ex)
		{
			log.warn("ProcessCalendar instance not found!");
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
	protected JbpmProcessContext getProcessContext(Process process)
	{
		var session = loadSession(process);

		return new JbpmProcessContext(process, session);
	}

	@Override
	protected boolean isProcessContextActive(JbpmProcessContext processContext)
	{
		var processInstance = processContext.getSession()
				.getProcessInstance(processContext.getProcessInstanceId());

		return processInstance instanceof WorkflowProcessInstance workflowProcessInstance
				&& workflowProcessInstance.getState() != ProcessInstance.STATE_COMPLETED;
	}

	@Override
	protected void startProcessInternal(JbpmProcessContext processContext)
	{
		var session = processContext.getSession();
		var process = processContext.getProcess();
		var processInstance = session.createProcessInstance(process.getWorkflowId(), process.getParameters());

		ProcessUtils.setProcessId(processInstance, process.getId());

		process.setInstanceId(Long.toString(processInstance.getId()));

		getProcessRepository().saveAndFlush(process);

		session.startProcessInstance(processInstance.getId());
	}

	@Override
	protected void startProcessOnEventInternal(JbpmProcessContext processContext, String event,
	                                           Map<String, Object> eventParameters, Runnable beforeStartTrigger)
	{
		var session = processContext.getSession();
		var process = processContext.getProcess();
		var processParameters = process.getParameters();

		session.addEventListener(new DefaultProcessEventListener() {
			@Override
			public void beforeProcessStarted(ProcessStartedEvent event)
			{
				if (!Objects.equals(process.getInstanceId(), UNDEFINED_INSTANCE_ID))
				{
					throw new BpmException("running more than one event based process at the same time is not supported");
				}

				process.setWorkflowId(event.getProcessInstance().getProcessId());
				process.setInstanceId(Long.toString(event.getProcessInstance().getId()));

				getProcessRepository().saveAndFlush(process);

				ProcessUtils.setProcessId(event.getProcessInstance(), process.getId());

				processParameters.forEach((key, value) -> ProcessUtils.setProcessVariable(event.getProcessInstance(), key, value));

				executeProcessScript(process, PROCESS_HANDLER_METHOD_START);
			}
		});

		session.signalEvent(event, eventParameters);
	}

	@Override
	protected void stopProcessInternal(JbpmProcessContext processContext)
	{
		var session = processContext.getSession();

		session.abortProcessInstance(processContext.getProcessInstanceId());
	}

	@Override
	public void killProcess(String processId)
	{
		var processContext = getProcessContext(processId);
		var process = processContext.getProcess();

		sessionInfoRepository.deleteById(process.getSessionId());
		workItemInfoRepository.deleteAllByProcessInstanceId(processContext.getProcessInstanceId());
		processInstanceInfoRepository.deleteById(processContext.getProcessInstanceId());

		jdbcTemplate.update("""
				delete from jbpm_correlationpropertyinfo
				where correlationkey_id in (select id from jbpm_correlationkeyinfo where processinstanceid = :id)
				""", Map.of("id", processContext.getProcessInstanceId()));

		jdbcTemplate.update("""
				delete from jbpm_correlationkeyinfo where processinstanceid = :id
				""", Map.of("id", processContext.getProcessInstanceId()));

		getTaskRepository().deleteByProcessId(processId);
		getProcessRepository().delete(process);
	}

	@Override
	public void deleteProcess(String processId)
	{
		var process = getProcessRepository().findByIdEx(processId);

		getTaskRepository().deleteByProcessId(processId);
		getProcessRepository().delete(process);

		if (!Objects.equals(process.getInstanceId(), UNDEFINED_INSTANCE_ID))
		{
			var processContext = getProcessContext(process);
			processContext.getSession()
					.destroy();
		}
	}

	@Override
	protected void completeTaskInternal(Task task, Map<String, Object> parameters)
	{
		var processContext = getProcessContext(task.getProcess());
		var workItemId = Long.parseLong(task.getWorkItemId());

		processContext.getSession()
				.getWorkItemManager()
				.completeWorkItem(workItemId, parameters);
	}

	@Override
	protected void cancelTaskInternal(Task task)
	{
		var processContext = getProcessContext(task.getProcess());
		var workItemId = Long.parseLong(task.getWorkItemId());

		processContext.getSession()
				.getWorkItemManager()
				.abortWorkItem(workItemId);
	}

	@Override
	protected void sendEventInternal(JbpmProcessContext processContext, String event, Map<String, Object> eventParameters)
	{
		var process = processContext.getProcess();
		var session = processContext.getSession();

		if (EVENT_TIMER_TRIGGERED.equals(event))
		{
			session.signalEvent(event, eventParameters.get(EVENT_PARAM_TIMER_INSTANCE), processContext.getProcessInstanceId());
		}
		else
		{
			executeProcessScript(process, PROCESS_HANDLER_METHOD_EVENT, event, eventParameters);

			session.signalEvent(event, eventParameters, processContext.getProcessInstanceId());
		}
	}

	@Override
	protected void updateProcessParametersInternal(JbpmProcessContext processContext)
	{
		var processInstance = processContext.getProcess();
		var session = processContext.getSession();

		session.execute(new SetProcessInstanceVariablesCommand(processContext.getProcessInstanceId(), processInstance.getParameters()));
	}

	@Override
	protected void migrateWorkflowInternal(JbpmProcessContext processContext, Workflow targetWorkflow)
	{
		var session = processContext.getSession();
		var processInstance = processContext.getProcess();

		var workflowId = targetWorkflow.getId() + "_" + targetWorkflow.getVersion();

		ProcessUtils.migrateProcess(processInstance, session.getProcessInstance(processContext.getProcessInstanceId()), workflowId);
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

	private KieSession loadSession(Process processInstance)
	{
		if (getProcessRepository().getEntityManager().contains(processInstance))
		{
			getProcessRepository().lock(processInstance, bpmProperties.isPessimisticLocking()
					? LockModeType.PESSIMISTIC_WRITE
					: LockModeType.OPTIMISTIC_FORCE_INCREMENT);
		}

		var env = EnvironmentFactory.newEnvironment();
		env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, em.getEntityManagerFactory());
		env.set(EnvironmentName.CMD_SCOPED_ENTITY_MANAGER, em);
		env.set(EnvironmentName.APP_SCOPED_ENTITY_MANAGER, em);
		env.set(EnvironmentName.TRANSACTION_MANAGER, transactionManager);
		env.set(EnvironmentName.PERSISTENCE_CONTEXT_MANAGER, new JpaProcessPersistenceContextManager(env));
		env.set(ENV_NAME_BUSINESS_CALENDAR, processCalendar);

		KieSession session;

		if (!Objects.equals(processInstance.getSessionId(), UNDEFINED_SESSION_ID))
		{
			session = JPAKnowledgeService.loadStatefulKnowledgeSession(processInstance.getSessionId(), knowledgeService.getKieBase(), createSessionConfiguration(), env);
		}
		else
		{
			session = JPAKnowledgeService.newStatefulKnowledgeSession(knowledgeService.getKieBase(), createSessionConfiguration(), env);

			processInstance.setSessionId(session.getIdentifier());
		}

		scriptingExtensionLocator.getScriptingExtensions(getEngineType())
				.forEach(ext -> session.setGlobal(ext.getBindingName(), ext));

		session.getWorkItemManager()
				.registerWorkItemHandler("Human Task", new HumanTaskHandler(processInstance, getTaskManager()));
		session.addEventListener(new VariableChangeEventListener(processInstance));

		session.execute(new DisposeCommand());

		return session;
	}

	private SessionConfiguration createSessionConfiguration()
	{
		return !customSessionProperties.isEmpty()
				? new CustomSessionConfiguration(customSessionProperties)
				: SessionConfiguration.newInstance();
	}
}
