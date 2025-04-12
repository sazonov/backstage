package workflows.order

import com.proit.bpm.jbpm.OrderTests
import com.proit.bpm.domain.Task

class PackHandler : BaseTaskHandler()
{
	override fun onTaskCreate(task: Task)
	{
		log.info("Упаковка заказа ${getOrderId(task.process)}.")

		setOrderStatus(task.process, OrderTests.OrderStatus.PACKING)

		addAction(task, "pack", "Упаковать заказ")
	}

	override fun onTaskComplete(task: Task)
	{
		log.info("Заказ ${getOrderId(task.process)} упакован.")
	}
}