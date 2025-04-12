package workflows.test

import com.proit.bpm.domain.Task
import com.proit.bpm.service.handler.AbstractTaskHandler
import groovy.util.logging.Slf4j

@Slf4j
class BaseTaskHandler extends AbstractTaskHandler
{
	def baseVariable = "Значение по умолчанию"

	@Override
	void onTaskComplete(Task task)
	{
		log.info("Вызов базового метода.")
	}
}
