package com.proit.bpm;

import com.proit.app.utils.TimeUtils;
import com.proit.bpm.domain.TaskStatus;
import com.proit.bpm.helper.TransactionHelper;
import com.proit.bpm.model.TaskFilter;
import com.proit.bpm.service.ProcessService;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@Order(1)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BpmTests extends AbstractTests
{
	@Autowired private AsyncTaskExecutor taskExecutor;

	@Autowired private TransactionHelper transactionHelper;
	@Autowired private ProcessService processService;

	@Test
	public void runNestedProcesses()
	{
		var processIds = new ArrayList<String>();

		transactionHelper.runInTransaction(() -> {
			processIds.add(processService.startProcess("test", Map.of(
					"documentId", "doc1"
			)).getId());

			transactionHelper.runInNewTransaction(() -> {
				processIds.add(processService.startProcess("test", Map.of(
						"documentId", "doc2"
				)).getId());
			});
		});

		processIds.forEach(processId -> processService.sendEvent(processId, "abortProcess", Map.of("data", "value")));

		assertEquals(processIds.size(), processIds.stream().map(processService::getProcess).filter(Optional::isPresent).count());
	}

	@Test
	public void boundaryTimerTest()
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
	public void eventDrivenStart()
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
	public void runProcess()
	{
		var process = processService.startProcess("test", Map.of(
				"documentId", "123"
		));

		var timers = processService.getProcessTimers(process.getId());

		assertEquals(timers.size(), 1);

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
				.statuses(Collections.singletonList(TaskStatus.PENDING))
				.processParameters(Map.of("documentId", "123"))
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

		process = processService.getProcess("documentId", "123").get();

		processService.sendEvent(process.getId(), "timerWaitForFeedback");
	}

	@Test
	public void runProcessWithTimers()
	{
		var process = processService.startProcess("test2", Map.of());

		TimeUtils.sleep(45 * 1000);

		process = processService.getProcess(process.getId()).get();

		assertFalse(process.isActive());
	}

	@Test
	public void runParallelProcesses() throws Exception
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
	public void testKillProcess()
	{
		var process = processService.startProcess("test", Map.of(
				"documentId", UUID.randomUUID().toString()
		));

		processService.killProcess(process.getId());

		assertTrue(processService.getProcess(process.getId()).isEmpty());
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
