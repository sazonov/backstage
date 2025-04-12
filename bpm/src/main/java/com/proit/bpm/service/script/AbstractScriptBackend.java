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

import com.proit.bpm.model.WorkflowScript;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import java.util.Arrays;

@RequiredArgsConstructor
public abstract class AbstractScriptBackend implements ScriptBackend
{
	private final WorkflowScript.Type supportedScriptType;

	private final AutowireCapableBeanFactory beanFactory;

	public boolean supports(WorkflowScript.Type type)
	{
		return type == supportedScriptType;
	}

	public Object execute(Script script, String methodName, Object... parameters)
	{
		try
		{
			var scriptObject = beanFactory.createBean(script.getClazz());
			var method = Arrays.stream(script.getClazz().getMethods())
					.filter(m -> m.getName().equals(methodName)).findFirst().orElseThrow(() -> {
						throw new RuntimeException("no such method: %s".formatted(methodName));
					});

			return method.invoke(scriptObject, parameters);
		}
		catch (Exception e)
		{
			throw new RuntimeException("failed to execute script '%s'".formatted(script.getId()), e);
		}
	}
}
