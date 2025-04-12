package workflows.test

import com.proit.bpm.domain.Task
import groovy.util.logging.Slf4j

@Slf4j
class Task1Handler extends BaseTaskHandler
{
	@Override
	void onTaskCreate(Task task)
	{
		log.info("$baseVariable")
		log.info("Проверяем условия для создания задачи в проекте '{}'.", task.process.parameters['documentId'])

		baseVariable = 3

		addAction(task, "accept", "Утвердить документ")
		addAction(task, "reject", "Отклонить документ")
	}

	@Override
	void onTaskComplete(Task task)
	{
		super.onTaskComplete(task)

		log.info("Проверяем условия для выполнения задачи.")
	}
}
