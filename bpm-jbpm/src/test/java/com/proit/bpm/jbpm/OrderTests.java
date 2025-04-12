package com.proit.bpm.jbpm;

import com.proit.bpm.model.TaskFilterHelper;
import com.proit.bpm.service.process.ProcessService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Order(2)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrderTests extends AbstractTest
{
	public enum OrderStatus
	{
		CHECKING,
		PACKING,
		DELIVERING,
		CLOSED
	}

	public static class ProcessConstants
	{
		public static final String ROLE_MANAGER = "manager";
		public static final String ROLE_WAREHOUSE_MANAGER = "warehouse manager";
		public static final String ROLE_WAREHOUSE_LEAD = "warehouse lead";
		public static final String ROLE_DELIVERY = "delivery";

		public static final String ACTION_MANAGER_CONFIRM = "confirm";
		public static final String ACTION_WAREHOUSE_PACK = "pack";
		public static final String ACTION_DELIVERY_CONFIRM = "confirm";
		public static final String ACTION_DELIVERY_REJECT = "reject";
	}

	@Slf4j
	@Service
	public static class OrderArchiveService
	{
		public void archiverOrder(Map<String, Object> orderDetails)
		{
			log.info("Заказ перемещён в архив: {}.", orderDetails);
		}
	}

	@Autowired private ProcessService processService;

	@Test
	public void runOrderTest()
	{
		var process = processService.startProcess("order", Map.of(
				"orderId", "1"
		));

		processService.getTask(TaskFilterHelper.pendingByRole(process, ProcessConstants.ROLE_MANAGER)).ifPresent(task -> {
			processService.completeTask(task, ProcessConstants.ACTION_MANAGER_CONFIRM);
		});

		assertTrue(processService.getTask(TaskFilterHelper.pendingByRole(process, ProcessConstants.ROLE_WAREHOUSE_MANAGER)).isPresent());

		processService.getTask(TaskFilterHelper.pendingByRole(process, ProcessConstants.ROLE_WAREHOUSE_LEAD)).ifPresent(task -> {
			processService.completeTask(task, ProcessConstants.ACTION_WAREHOUSE_PACK);
		});

		processService.getTask(TaskFilterHelper.pendingByRole(process, ProcessConstants.ROLE_DELIVERY)).ifPresent(task -> {
			processService.completeTask(task, ProcessConstants.ACTION_DELIVERY_REJECT);
		});

		processService.getTask(TaskFilterHelper.pendingByRole(process, ProcessConstants.ROLE_DELIVERY)).ifPresent(task -> {
			processService.completeTask(task, ProcessConstants.ACTION_DELIVERY_CONFIRM);
		});

		process = processService.getProcess(process.getId()).orElseThrow();

		assertEquals(process.getParameters().get("status"), OrderStatus.CLOSED.name());
	}
}
