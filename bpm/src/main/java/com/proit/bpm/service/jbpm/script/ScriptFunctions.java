/*
 *    Copyright 2019-2022 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.proit.bpm.service.jbpm.script;

import com.proit.bpm.domain.Process;
import com.proit.bpm.exception.BpmException;
import com.proit.bpm.service.jbpm.ProcessUtils;
import com.proit.bpm.service.script.ScriptEngine;
import com.proit.bpm.service.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.process.ProcessContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScriptFunctions implements ScriptingExtension
{
	private final WorkflowService workflowService;
	private final ProcessUtils processUtils;
	private final ScriptEngine scriptEngine;

	public Object invokeScript(ProcessContext processContext, String scriptName, Object... args)
	{
		log.info("Executing script '{}'.", scriptName);

		var process = processUtils.getProcess(processContext.getProcessInstance());

		return executeScript(process, scriptName, args);
	}

	private Object executeScript(Process process, String scriptName, Object... args)
	{
		var script = workflowService.getWorkflowScript(process.getWorkflowId(), scriptName + "Script");

		if (script == null)
		{
			throw new BpmException(String.format("failed to find process script '%s'", scriptName));
		}

		var scriptArgs = new ArrayList<>(args.length + 1);
		scriptArgs.add(process);
		scriptArgs.addAll(Arrays.asList(args));

		return scriptEngine.execute(script, "execute", scriptArgs.toArray());
	}
}
