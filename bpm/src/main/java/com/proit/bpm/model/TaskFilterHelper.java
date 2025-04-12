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

package com.proit.bpm.model;

import com.proit.bpm.domain.Process;
import com.proit.bpm.domain.TaskStatus;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class TaskFilterHelper
{
	public TaskFilter byRoleAndStatus(String processId, String userRole, TaskStatus taskStatus)
	{
		return TaskFilter.builder()
				.processId(processId)
				.userRole(userRole)
				.statuses(List.of(taskStatus))
				.build();
	}

	public TaskFilter byRoleAndStatus(Process process, String userRole, TaskStatus taskStatus)
	{
		return byRoleAndStatus(process.getId(), userRole, taskStatus);
	}

	public TaskFilter pendingByRole(Process process, String userRole)
	{
		return byRoleAndStatus(process, userRole, TaskStatus.PENDING);
	}
}
