package workflows.order

import com.proit.bpm.OrderTests
import com.proit.bpm.domain.Task

class CheckHandler : BaseTaskHandler()
{
	override fun onTaskCreate(task: Task)
	{
		log.info("Проверка заказа ${getOrderId(task.process)}.")

		setOrderStatus(task.process, OrderTests.OrderStatus.CHECKING)

		addAction(task, OrderTests.ProcessConstants.ACTION_MANAGER_CONFIRM, "Подтвердить заказ")
	}
}