package workflows.order

import com.proit.bpm.service.handler.AbstractTaskHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class BaseTaskHandler : AbstractTaskHandler(), OrderOperations
{
	protected val log: Logger = LoggerFactory.getLogger(this.javaClass)
}