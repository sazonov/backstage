package workflows.test

import com.proit.bpm.domain.Process
import com.proit.bpm.service.handler.AbstractProcessHandler
import groovy.util.logging.Slf4j

@Slf4j
class ProcessHandler extends AbstractProcessHandler
{
	@Override
	void onProcessEvent(Process process, String event, Map<String, Object> eventParameters)
	{
		super.onProcessEvent(process, event, eventParameters)
	}

	@Override
	void onProcessStop(Process process)
	{
		log.info("Процесс завершён.")
	}
}
