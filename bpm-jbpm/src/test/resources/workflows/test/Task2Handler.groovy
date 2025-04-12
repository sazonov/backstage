package workflows.test

import com.proit.bpm.domain.Task
import groovy.util.logging.Slf4j

@Slf4j
class Task2Handler extends BaseTaskHandler
{
	@Override
	void onTaskCreate(Task task)
	{
		addAction(task, "complete", "Завершить задачу")
	}

	@Override
	void onTaskComplete(Task task)
	{
		log.info("Задача завершена.")
	}
}
