/*
 *    Copyright 2019-2024 the original author or authors.
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

package com.proit.bpm.jbpm.service.script.extension;

import com.proit.bpm.exception.BpmException;
import com.proit.bpm.model.EngineType;
import com.proit.bpm.repository.ProcessRepository;
import com.proit.bpm.service.script.ScriptEngine;
import com.proit.bpm.service.script.extension.AbstractScriptFunctions;
import com.proit.bpm.service.workflow.WorkflowService;
import org.kie.api.runtime.process.ProcessContext;
import org.springframework.stereotype.Component;

@Component
public class JbpmScriptFunctions extends AbstractScriptFunctions
{
	private final ProcessRepository processRepository;

	public JbpmScriptFunctions(WorkflowService workflowService, ScriptEngine scriptEngine, ProcessRepository processRepository)
	{
		super(workflowService, scriptEngine);

		this.processRepository = processRepository;
	}

	public Object invokeScript(ProcessContext processContext, String scriptName, Object... args)
	{
		var process = processRepository.findByInstanceId(Long.valueOf(processContext.getProcessInstance().getId()).toString()).orElseThrow(() ->
			new BpmException("cannot find process for jbpm process instance %d".formatted(processContext.getProcessInstance().getId()))
		);

		return executeScript(process, scriptName, args);
	}

	@Override
	public boolean supports(EngineType engineType)
	{
		return EngineType.JBPM.equals(engineType);
	}
}
