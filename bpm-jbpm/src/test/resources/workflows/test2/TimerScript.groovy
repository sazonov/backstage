package workflows.test2

import com.proit.bpm.domain.Process
import com.proit.bpm.service.handler.AbstractScriptHandler
import groovy.util.logging.Slf4j

@Slf4j
class TimerScript extends AbstractScriptHandler
{
	Integer execute(Process process, Integer fireCount)
	{
		fireCount = fireCount ?: 0
		fireCount++

		log.info("Таймер сработал $fireCount раз.")

		return fireCount
	}
}
