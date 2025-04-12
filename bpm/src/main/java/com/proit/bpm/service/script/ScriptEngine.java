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

package com.proit.bpm.service.script;

import com.proit.app.exception.AppException;
import com.proit.app.utils.ExceptionUtils;
import com.proit.bpm.model.WorkflowScript;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScriptEngine
{
	private final ScriptManager scriptManager;

	public Object execute(WorkflowScript workflowScript, Object... parameters)
	{
		return execute(workflowScript, "run", parameters);
	}

	public Object execute(WorkflowScript workflowScript, String method, Object... parameters)
	{
		try
		{
			return scriptManager.getScript(workflowScript).execute(method, parameters);
		}
		catch (Exception e)
		{
			ExceptionUtils.throwExtracted(e, AppException.class, new RuntimeException("failed to execute script", e));
		}

		return null;
	}
}
