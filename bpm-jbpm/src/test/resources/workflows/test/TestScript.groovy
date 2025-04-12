package workflows.test

import com.proit.bpm.domain.Process
import com.proit.bpm.service.handler.AbstractScriptHandler

class TestHandler extends AbstractScriptHandler
{
	@Override
	Object execute(Process process)
	{
		return true
	}
}
