package com.proit.bpm.jbpm;

import com.proit.app.utils.TimeUtils;
import com.proit.bpm.domain.TaskStatus;
import com.proit.bpm.model.TaskFilter;
import com.proit.bpm.jbpm.helper.TransactionHelper;
import com.proit.bpm.jbpm.repository.WorkItemInfoRepository;
import com.proit.bpm.service.process.ProcessService;
import com.proit.bpm.service.task.TaskManager;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@Order(1)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JbpmTests extends AbstractTest
{
	static final String WORKFLOW_ID = "test";
	static final String PROCESS_DOCUMENT_ID = "documentId";

	@Qualifier("taskScheduler")
	@Autowired private AsyncTaskExecutor taskExecutor;

	@Autowired private TransactionHelper transactionHelper;
	@Autowired private ProcessService processService;

	@Autowired private TaskManager taskManager;

	@Autowired private WorkItemInfoRepository workItemInfoRepository;

	@Test
	void runNestedProcesses()
	{
		var processIds = new ArrayList<String>();

		transactionHelper.runInTransaction(() -> {
			processIds.add(processService.startProcess(WORKFLOW_ID, Map.of(
					PROCESS_DOCUMENT_ID, "doc1"
			)).getId());

			transactionHelper.runInNewTransaction(() -> {
				processIds.add(processService.startProcess(WORKFLOW_ID, Map.of(
						PROCESS_DOCUMENT_ID, "doc2"
				)).getId());
			});
		});

		processIds.forEach(processId -> processService.sendEvent(processId, "abortProcess", Map.of("data", "value")));

		assertEquals(processIds.size(), processIds.stream().map(processService::getProcess).filter(Optional::isPresent).count());
	}

	@Test
	void terminatingSubProcessTest()
	{
		var process = processService.startProcess("terminatingSubProcess");

		assertFalse(process.isActive());
	}

	@Test
	void nonTerminatingSubProcessTest()
	{
		var process = processService.startProcess("nonTerminatingSubProcess");

		assertTrue(process.isActive());
	}

	@Test
	void stopProcessWithPendingTimerTest()
	{
		var process = processService.startProcess("boundaryTimer");

		processService.stopProcess(process.getId());
	}

	@Test
	void boundaryTimerTest()
	{
		var process = processService.startProcess("boundaryTimer");
		var timers = processService.getProcessTimers(process.getId());

		assertFalse(timers.isEmpty());

		processService.sendEvent(process, "skipBoundaryTimer1");

		timers = processService.getProcessTimers(process.getId());

		assertFalse(timers.isEmpty());

		processService.sendEvent(process, "skipBoundaryTimer2");

		timers = processService.getProcessTimers(process.getId());

		assertTrue(timers.isEmpty());
	}

	@Test
	void eventDrivenStart()
	{
		var paramName = "objectId";
		var paramValue = 1;

		var process = processService.startProcessOnEvent("startSignal1", Map.of(paramName, paramValue), Map.of(paramName, paramValue));

		assertTrue(process.isPresent());
		assertTrue(process.get().isActive());
		assertEquals(process.get().getParameters().get(paramName), paramValue);

		process = processService.startProcessOnEvent("startSignal2", Map.of());

		assertTrue(process.isPresent());
		assertFalse(process.get().isActive());
		assertTrue(process.get().getParameters().isEmpty());

		process = processService.startProcessOnEvent("startSignal3", Map.of());

		assertFalse(process.isPresent());
	}

	@Test
	void syncProcessParams()
	{
		var paramName = "processVar1";

		var process = processService.startProcess("processParams", Map.of(paramName, false));
		process = processService.getProcess(process.getId()).orElse(null);

		assertNotNull(process);
		assertTrue((Boolean) process.getParameters().get(paramName));
	}

	@Test
	void runProcess()
	{
		var documentId = UUID.randomUUID().toString();

		var process = processService.startProcess(WORKFLOW_ID, Map.of(
				PROCESS_DOCUMENT_ID, documentId
		));

		var timers = processService.getProcessTimers(process.getId());

		assertEquals(1, timers.size());

		var timerName = "Задача 1#1";
		var timeNewFireTime = LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.MILLIS);
		var timer = timers.stream().filter(t -> timerName.equals(t.getName())).findFirst();

		assertTrue(timer.isPresent());

		processService.updateProcessTimer(process.getId(), timer.get().getName(), timeNewFireTime);

		timer = processService.getProcessTimers(process.getId()).stream().filter(t -> timerName.equals(t.getName())).findFirst();

		assertTrue(timer.isPresent());
		assertEquals(timer.get().getNextFireTime(), timeNewFireTime);

		var filter = TaskFilter.builder()
				.process(process)
				.userId("john")
				.statuses(List.of(TaskStatus.PENDING))
				.processParameters(Map.of(PROCESS_DOCUMENT_ID, documentId))
				.build();

		var task = processService.getTasks(filter, PageRequest.of(0, 10)).getContent().get(0);

		processService.completeTask(task.getId(), "accept", Map.of(
				"result", true,
				"comment", "Проверено успешно."
		));

		filter = TaskFilter.builder()
				.process(process)
				.userRole("PM")
				.build();

		task = processService.getTasks(filter, PageRequest.of(0, 10)).getContent().get(0);

		processService.assignTask(task.getId(), task.getCandidateUserIds().get(0));
		processService.completeTask(task.getId(), "complete", Map.of(
				"comment", "Успешно выбрали одного из кандидатов для задачи."
		));

		processService.sendEvent(process, "abortProcess", Map.of("data", "value"));

		TimeUtils.sleep(5 * 1000);

		process = processService.getProcess(PROCESS_DOCUMENT_ID, documentId).get();

		processService.sendEvent(process.getId(), "timerWaitForFeedback");
	}

	@Test
	void stopProcess()
	{
		var process = processService.startProcess(WORKFLOW_ID, Map.of(
				PROCESS_DOCUMENT_ID, UUID.randomUUID().toString()
		));

		processService.stopProcess(process.getId());

		var filter = TaskFilter.builder()
				.statuses(List.of(TaskStatus.PENDING))
				.build();

		var tasks = taskManager.getTasks(filter, Pageable.unpaged()).getContent();

		tasks.forEach(task -> assertEquals(TaskStatus.CANCELED, task.getStatus()));
	}

	@Test
	void runProcessWithTimers()
	{
		var process = processService.startProcess("test2", Map.of());

		TimeUtils.sleep(45 * 1000);

		process = processService.getProcess(process.getId()).get();

		assertFalse(process.isActive());
	}

	@Test
	void runParallelProcesses() throws Exception
	{
		List<Future<?>> futures = new LinkedList<>();

		for (int i = 0; i < 500; i++)
		{
			var processNumber = i;

			futures.add(taskExecutor.submit(() -> executeParallelProcess(processNumber + 1)));
		}

		for (var future : futures)
		{
			future.get();
		}
	}

	@Test
	void killProcess()
	{
		var process = processService.startProcess(WORKFLOW_ID, Map.of(
				PROCESS_DOCUMENT_ID, UUID.randomUUID().toString()
		));

		processService.killProcess(process.getId());

		assertTrue(processService.getProcess(process.getId()).isEmpty());
	}

	@Test
	void killProcessWithMissingWorkItem()
	{
		var process = processService.startProcess(WORKFLOW_ID, Map.of(
				PROCESS_DOCUMENT_ID, UUID.randomUUID().toString()
		));

		var instanceId = process.getInstanceId();

		transactionHelper.runInTransaction(() ->
				workItemInfoRepository.deleteAllByProcessInstanceId(Long.parseLong(instanceId)));

		processService.killProcess(process.getId());

		assertTrue(processService.getProcess(process.getId()).isEmpty());
		assertTrue(workItemInfoRepository.findAllByProcessInstanceId(Long.parseLong(instanceId)).isEmpty());
	}

	private void executeParallelProcess(int processNumber)
	{
		var process = processService.startProcess("test3", Map.of());

		var filter = TaskFilter.builder()
				.process(process)
				.build();

		var task = processService.getTasks(filter, PageRequest.of(0, 10)).getContent().get(0);

		processService.completeTask(task.getId(), "complete", Map.of("processNumber", processNumber));
	}
}
