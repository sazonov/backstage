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

package com.proit.bpm.service.script.groovy;

import com.proit.app.utils.FileUtils;
import com.proit.bpm.model.WorkflowScript;
import groovy.lang.GroovyClassLoader;
import org.apache.commons.lang3.StringUtils;

public class ScriptClassLoader extends GroovyClassLoader
{
	public ScriptClassLoader(ClassLoader parent)
	{
		super(parent);
	}

	public Class<?> parseClass(WorkflowScript script)
	{
		try
		{
			setResourceLoader(filename -> {
				var module = script.getWorkflow().getScripts().get(getScriptName(filename));

				if (module != null)
				{
					return FileUtils.writeUnicodeStringToTempFile(module.getDefinition()).toUri().toURL();
				}

				return null;
			});

			return parseClass(script.getDefinition(), script.getId() + ".groovy");
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	private String getScriptName(String className)
	{
		String[] scriptParts = StringUtils.split(className, ".");

		return scriptParts[scriptParts.length - 1];
	}
}
