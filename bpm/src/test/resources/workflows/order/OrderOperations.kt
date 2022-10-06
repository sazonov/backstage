package workflows.order

import com.proit.bpm.OrderTests
import com.proit.bpm.domain.Process

interface OrderOperations
{
	fun getOrderId(process: Process) : String
	{
		return process.parameters["orderId"] as String
	}

	fun setOrderStatus(process: Process, orderStatus: OrderTests.OrderStatus)
	{
		process.parameters["status"] = orderStatus
	}

	fun getDeliveryCount(process: Process) : Int
	{
		return process.parameters["deliveryTryCount"] as? Int ?: 0
	}

	fun increaseDeliveryTryCount(process: Process)
	{
		process.parameters["deliveryTryCount"] = getDeliveryCount(process) + 1
	}
}