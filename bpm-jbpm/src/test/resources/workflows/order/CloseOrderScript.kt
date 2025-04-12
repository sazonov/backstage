package workflows.order

import com.proit.bpm.jbpm.OrderTests
import com.proit.bpm.jbpm.OrderTests.OrderArchiveService
import com.proit.bpm.domain.Process
import com.proit.bpm.service.handler.AbstractScriptHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CloseOrderScript(private val orderArchiveService: OrderArchiveService) : AbstractScriptHandler(), OrderOperations
{
	val log: Logger = LoggerFactory.getLogger(this.javaClass)

	override fun execute(process: Process)
	{
		log.info("Заказ ${getOrderId(process)} обработан.")

		setOrderStatus(process, OrderTests.OrderStatus.CLOSED)

		orderArchiveService.archiverOrder(mapOf(
			"orderId" to getOrderId(process),
			"deliveryTryCount" to getDeliveryCount(process),
			"deliveryDate" to process.parameters["deliveryDate"]
		))
	}
}