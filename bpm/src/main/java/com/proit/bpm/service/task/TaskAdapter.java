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

package com.proit.bpm.service.task;

import com.proit.bpm.domain.Process;
import com.proit.bpm.domain.Task;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TaskAdapter
{
	public static class TaskProperties
	{
		public static final String TYPE = "TaskName";
		public static final String NAME = "NodeName";
		public static final String COMMENT = "Comment";
		public static final String LOCALE = "Locale";
		public static final String PRIORITY = "Priority";
		public static final String CREATED_BY = "CreatedBy";
		public static final String SKIPPABLE = "Skippable";
		public static final String USER_ID = "ActorId";
		public static final String USER_ROLE = "GroupId";
		public static final String SWIMLANE_ACTOR_ID = "SwimlaneActorId";
	}

	private static final Set<String> SYSTEM_TASK_PROPERTIES = Set.of(
			TaskProperties.TYPE,
			TaskProperties.NAME,
			TaskProperties.COMMENT,
			TaskProperties.LOCALE,
			TaskProperties.PRIORITY,
			TaskProperties.CREATED_BY,
			TaskProperties.SKIPPABLE,
			TaskProperties.USER_ID,
			TaskProperties.USER_ROLE,
			TaskProperties.SWIMLANE_ACTOR_ID
	);

	public static Task createTask(Process process, String workItemId, Map<String, Object> parameters)
	{
		Task task = new Task();

		task.setCreated(LocalDateTime.now());
		task.setProcess(process);
		task.setWorkItemId(workItemId);
		task.setType((String) parameters.get(TaskProperties.TYPE));
		task.setName((String) parameters.get(TaskProperties.NAME));
		task.setComment((String) parameters.get(TaskProperties.COMMENT));

		var userCandidates = ((String) parameters.getOrDefault(TaskProperties.USER_ID, "")).split(",");

		if (userCandidates.length > 0)
		{
			if (userCandidates.length > 1)
			{
				task.setCandidateUserIds(Arrays.stream(userCandidates).collect(Collectors.toList()));
			}
			else
			{
				task.setUserId(userCandidates[0]);
			}
		}

		task.setUserRoles(Arrays.stream(((String) parameters.getOrDefault(TaskProperties.USER_ROLE, "")).split(",")).collect(Collectors.toList()));

		var taskParameters = new HashMap<>(parameters);

		SYSTEM_TASK_PROPERTIES.forEach(taskParameters::remove);

		task.setParameters(taskParameters);

		return task;
	}
}
