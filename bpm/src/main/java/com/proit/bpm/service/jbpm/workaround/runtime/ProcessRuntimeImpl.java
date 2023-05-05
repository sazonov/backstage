/*
 *    Copyright 2019-2023 the original author or authors.
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

package com.proit.bpm.service.jbpm.workaround.runtime;

import org.drools.core.common.InternalKnowledgeRuntime;
import org.drools.core.common.InternalWorkingMemory;

public class ProcessRuntimeImpl extends org.jbpm.process.instance.ProcessRuntimeImpl
{
	private final org.jbpm.process.instance.timer.TimerManager timerManager;

	public ProcessRuntimeImpl(InternalKnowledgeRuntime kruntime)
	{
		super(kruntime);

		timerManager = new TimerManager(kruntime, kruntime.getTimerService());
	}

	public ProcessRuntimeImpl(InternalWorkingMemory workingMemory)
	{
		super(workingMemory);

		var knowledgeRuntime = workingMemory.getKnowledgeRuntime();

		timerManager = new TimerManager(knowledgeRuntime, knowledgeRuntime.getTimerService());
	}

	@Override
	public org.jbpm.process.instance.timer.TimerManager getTimerManager()
	{
		return timerManager;
	}
}
