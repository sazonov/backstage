package workflows.test

import com.proit.bpm.domain.Task
import groovy.util.logging.Slf4j

@Slf4j
class EventTaskHandler extends BaseTaskHandler
{
	@Override
	void onTaskCreate(Task task)
	{
		task.parameters
	}

	@Override
	void onTaskComplete(Task task)
	{
		log.info("Проверяем условия для выполнения задачи.")
	}
}
