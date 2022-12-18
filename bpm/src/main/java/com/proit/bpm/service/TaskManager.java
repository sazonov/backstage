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

/*
 * Интеллектуальная собственность ООО "Про Ай-Ти Ресурс".
 * Использование ограничено прилагаемой лицензией.
 */

package com.proit.bpm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proit.app.utils.DataUtils;
import com.proit.bpm.domain.Task;
import com.proit.bpm.domain.TaskResult;
import com.proit.bpm.domain.TaskStatus;
import com.proit.bpm.exception.BpmException;
import com.proit.bpm.model.TaskFilter;
import com.proit.bpm.repository.TaskRepository;
import com.proit.bpm.service.script.ScriptEngine;
import com.proit.bpm.service.workflow.WorkflowService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Boolean.TRUE;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TaskManager
{
	private static final String TASK_HANDLER_METHOD_CREATE = "onTaskCreate";
	private static final String TASK_HANDLER_METHOD_ASSIGN = "onTaskAssign";
	private static final String TASK_HANDLER_METHOD_COMPLETE = "onTaskComplete";
	private static final String TASK_HANDLER_METHOD_ABORT = "onTaskAbort";

	private static final String MACRO_REGEX = "^%(.+)%$";
	private static final Pattern MACRO_PATTERN = Pattern.compile(MACRO_REGEX);

	private final NamedParameterJdbcTemplate jdbcTemplate;

	@Getter
	private final TaskRepository taskRepository;

	private final ScriptEngine scriptEngine;
	private final WorkflowService workflowService;

	private final ObjectMapper objectMapper;

	public Task createTask(Task task)
	{
		if (task.getId() != null)
		{
			throw new BpmException("cannot create task with id assigned");
		}

		if (task.getType() == null)
		{
			throw new BpmException("cannot create task without type");
		}

		if (!TRUE.equals(task.getProcess().isActive()))
		{
			throw new BpmException("cannot add task to inactive process");
		}

		// Разворачиваем макросы в параметрах.
		for (var entry : task.getParameters().entrySet())
		{
			final Matcher matcher = MACRO_PATTERN.matcher(entry.getValue().toString());

			if (matcher.matches())
			{
				var param = task.getProcess().getParameters().get(matcher.group(1));

				if (param == null)
				{
					log.warn("Cannot resolve macro parameter '{}' while creating task.", matcher.group(1));

					entry.setValue(null);
				}
				else
				{
					entry.setValue(param);
				}
			}
		}

		task.setStatus(TaskStatus.PENDING);
		task = taskRepository.save(task);

		executeTaskScript(task, TASK_HANDLER_METHOD_CREATE);

		log.info("Task created. Id: '{}', name: '{}', process id: '{}'.", task.getId(), task.getName(), task.getProcessId());

		return task;
	}

	public void completeTask(String taskId, TaskResult result)
	{
		var task = taskRepository.findByIdEx(taskId);

		if (task.getUserId() == null)
		{
			throw new BpmException(String.format("task '%s' is not assigned", task.getId()));
		}

		if (task.getStatus() != TaskStatus.PENDING)
		{
			throw new BpmException(String.format("task '%s' is not active", task.getId()));
		}

		log.info("Task completed. Id: '{}', name: '{}', process id: '{}'.", task.getId(), task.getName(), task.getProcessId());

		task.getActions().stream().filter(a -> a.getId().equals(result.getActionId())).findFirst()
				.orElseThrow(() -> new BpmException(String.format("task '%s' doesn't support action '%s'", task.getId(), result.getActionId())));

		task.setResult(result);
		task.setStatus(TaskStatus.COMPLETE);
		task.setCompleted(LocalDateTime.now());

		executeTaskScript(task, TASK_HANDLER_METHOD_COMPLETE);
	}

	public void cancelTask(String taskId)
	{
		var task = taskRepository.findByIdEx(taskId);

		log.info("Task canceled. Id: '{}', name: '{}', process id: '{}'.", task.getId(), task.getName(), task.getProcessId());

		if (task.getStatus() != TaskStatus.PENDING)
		{
			throw new BpmException(String.format("task '%s' is not active", task.getId()));
		}

		task.setStatus(TaskStatus.CANCELED);

		executeTaskScript(task, TASK_HANDLER_METHOD_ABORT);
	}

	public void assignTask(String taskId, String userId)
	{
		var task = taskRepository.findByIdEx(taskId);

		if (task.getStatus() != TaskStatus.PENDING)
		{
			throw new BpmException(String.format("task '%s' is not active", task.getId()));
		}

		log.info("Task assigned. Id: '{}', name: '{}', process id: '{}'.", task.getId(), task.getName(), task.getProcessId());

		if (StringUtils.isBlank(userId))
		{
			throw new BpmException("user id must be specified");
		}

		if (userId.equals(task.getUserId()))
		{
			return;
		}

		task.setUserId(userId);

		executeTaskScript(task, TASK_HANDLER_METHOD_ASSIGN);
	}

	public Page<Task> getTasks(TaskFilter taskFilter, Pageable pageable)
	{
		try
		{
			var whereClauses = new LinkedList<String>();
			var parameters = new MapSqlParameterSource();

			whereClauses.add("true");

			if (taskFilter.getProcessId() != null)
			{
				whereClauses.add("p.id = :processId");
				parameters.addValue("processId", taskFilter.getProcessId());
			}

			if (taskFilter.getUserId() != null)
			{
				whereClauses.add("(t.user_id = :userId or (t.user_id is null and (:userId = any(t.candidate_user_ids))))");
				parameters.addValue("userId", taskFilter.getUserId());
			}

			if (taskFilter.getUserRole() != null)
			{
				whereClauses.add(":userRole = any(t.user_roles)");

				parameters.addValue("userRole", taskFilter.getUserRole());
			}

			if (taskFilter.getType() != null)
			{
				whereClauses.add("t.type = :type");
				parameters.addValue("type", taskFilter.getType());
			}

			if (taskFilter.getStatuses() != null && !taskFilter.getStatuses().isEmpty())
			{
				whereClauses.add("t.status in (:statuses)");

				parameters.registerSqlType("statuses", Types.VARCHAR);
				parameters.addValue("statuses", taskFilter.getStatuses());
			}

			if (taskFilter.getProcessParameters() != null && !taskFilter.getProcessParameters().isEmpty())
			{
				whereClauses.add("p.parameters @> cast(:processParameters as jsonb)");
				parameters.addValue("processParameters", objectMapper.writerFor(Map.class).writeValueAsString(taskFilter.getProcessParameters()));
			}

			if (taskFilter.getTaskParameters() != null && !taskFilter.getTaskParameters().isEmpty())
			{
				whereClauses.add("t.parameters @> cast(:taskParameters as jsonb)");
				parameters.addValue("taskParameters", objectMapper.writerFor(Map.class).writeValueAsString(taskFilter.getTaskParameters()));
			}

			var commonSql = "from task t join process p on t.fk_process = p.id where " + String.join(" and ", whereClauses);

			var itemSql = "select t.id " + commonSql + " order by t.id";
			var itemIds = jdbcTemplate.queryForList(itemSql, parameters, String.class);

			if (itemIds.isEmpty())
			{
				return DataUtils.emptyPage(pageable);
			}

			var items = taskRepository.findAll(itemIds);

			if (pageable.isPaged())
			{
				return PageableExecutionUtils.getPage(items, pageable, items::size);
			}

			return new PageImpl<>(items);
		}
		catch (JsonProcessingException e)
		{
			throw new BpmException("failed to parse task filter", e);
		}
	}

	private void executeTaskScript(Task task, String method)
	{
		var script = workflowService.getWorkflowScript(task.getProcess().getWorkflowId(), task.getType() + "Handler");

		if (script != null)
		{
			scriptEngine.execute(script, method, task);
		}
	}
}