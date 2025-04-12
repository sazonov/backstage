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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.proit.bpm.configuration.properties.BpmProperties;
import com.proit.bpm.exception.BpmException;
import com.proit.bpm.model.WorkflowScript;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScriptManager
{
	private final List<ScriptBackend> backends;

	private final BpmProperties bpmProperties;

	private LoadingCache<WorkflowScript, Script> scriptCache;

	@PostConstruct
	public void initialize()
	{
		scriptCache = CacheBuilder.newBuilder()
				.maximumSize(bpmProperties.getScriptCacheSize())
				.softValues()
				.build(CacheLoader.from(this::loadScript));
	}

	public Script getScript(WorkflowScript workflowScript)
	{
		try
		{
			return scriptCache.get(workflowScript);
		}
//		catch (ScriptException e)
//		{
//			throw e;
//		}
		catch (Exception e)
		{
			throw new BpmException(String.format("failed to load script '%s'", workflowScript.getId()), e);
		}
	}

	private Script loadScript(WorkflowScript workflowScript)
	{
		synchronized (workflowScript)
		{
			ScriptBackend backend = backends.stream().filter(it -> it.supports(workflowScript.getType())).findFirst().orElseThrow(
					() -> new RuntimeException(String.format("failed to find backend for script '%s'", workflowScript.getId())));

			log.info("Compiling '{}'.", workflowScript.getId());

			return backend.compile(workflowScript);
		}
	}
}
