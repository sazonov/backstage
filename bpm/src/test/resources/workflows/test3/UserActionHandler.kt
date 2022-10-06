package workflows.test3

import com.proit.bpm.domain.Task
import com.proit.bpm.service.ProcessService
import com.proit.bpm.service.handler.AbstractTaskHandler
import org.slf4j.LoggerFactory

class UserActionHandler(processService: ProcessService) : AbstractTaskHandler()
{
	private val log = LoggerFactory.getLogger(this.javaClass)

	override fun onTaskCreate(task: Task)
	{
		addAction(task, "complete", uppercase("Выполнить действие"))
	}

	override fun onTaskComplete(task: Task)
	{
		log.info("Действие выполнено в процессе ${task.result.parameters["processNumber"]}.")
	}
}
