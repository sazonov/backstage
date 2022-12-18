package workflows.eventDrivenStart

import com.proit.bpm.domain.Process
import groovy.util.logging.Slf4j

@Slf4j
class SetupContextScript
{
	def execute(Process process, Map<String, Object> parameters)
	{
		log.info("Параметры процесса: $parameters.")
	}
}
