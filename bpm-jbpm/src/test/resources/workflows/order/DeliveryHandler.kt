package workflows.order

import com.proit.bpm.jbpm.OrderTests
import com.proit.bpm.domain.Task
import java.time.LocalDateTime

class DeliveryHandler : BaseTaskHandler()
{
	override fun onTaskCreate(task: Task)
	{
		val deliveryCount = getDeliveryCount(task.process)

		if (deliveryCount > 0)
		{
			log.info("Повторная доставка заказа ${getOrderId(task.process)}.")
		}
		else
		{
			log.info("Доставка заказа ${getOrderId(task.process)}.")
		}

		setOrderStatus(task.process, OrderTests.OrderStatus.DELIVERING)

		addAction(task, OrderTests.ProcessConstants.ACTION_DELIVERY_CONFIRM, "Доставить заказ")
		addAction(task, OrderTests.ProcessConstants.ACTION_DELIVERY_REJECT, "Отправить на повторную доставку")
	}

	override fun onTaskComplete(task: Task)
	{
		increaseDeliveryTryCount(task.process)

		if (task.result.actionId == "confirm")
		{
			log.info("Заказ ${getOrderId(task.process)} доставлен.")

			task.process.parameters["deliveryDate"] = LocalDateTime.now()
		}
		else
		{
			log.info("Не удалось доставить заказ ${getOrderId(task.process)}.")
		}
	}
}