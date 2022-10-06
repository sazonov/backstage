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

package com.proit.bpm.model;

import com.proit.app.model.domain.Identity;
import com.proit.bpm.domain.Process;
import com.proit.bpm.domain.TaskStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class TaskFilter
{
	private final String processId;

	private final String userId;

	private final String userRole;

	private final String type;

	private final List<TaskStatus> statuses;

	private final Map<String, Object> processParameters;

	private final Map<String, Object> taskParameters;

	public static class TaskFilterBuilder
	{
		public TaskFilterBuilder process(Process process)
		{
			this.processId = process.getId();

			return this;
		}

		public TaskFilterBuilder type(Enum<?> type)
		{
			return type(type.name());
		}

		public TaskFilterBuilder type(String type)
		{
			this.type = type;

			return this;
		}

		public TaskFilterBuilder user(Identity<String> user)
		{
			this.userId = user.getId();

			return this;
		}

		public TaskFilterBuilder userRole(Enum<?> userRole)
		{
			return userRole(userRole.name());
		}

		public TaskFilterBuilder userRole(String userRole)
		{
			this.userRole = userRole;

			return this;
		}
	}
}
