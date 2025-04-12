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

package com.proit.bpm.jbpm.service;

import com.proit.bpm.domain.Process;
import com.proit.bpm.service.task.TaskAdapter;
import com.proit.bpm.service.task.TaskManager;
import lombok.AllArgsConstructor;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

@AllArgsConstructor
public class HumanTaskHandler implements WorkItemHandler
{
	private final Process processInstance;

	private final TaskManager taskManager;

	public void executeWorkItem(WorkItem workItem, WorkItemManager workItemManager)
	{
		taskManager.createTask(TaskAdapter.createTask(processInstance, Long.valueOf(workItem.getId()).toString(), workItem.getParameters()));
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager workItemManager)
	{
		var task = taskManager.getTaskRepository().findByWorkItemId(Long.toString(workItem.getId()));

		if (task == null)
		{
			throw new RuntimeException(String.format("failed to find task for work item %d", workItem.getId()));
		}

		taskManager.cancelTask(task.getId());
	}
}
