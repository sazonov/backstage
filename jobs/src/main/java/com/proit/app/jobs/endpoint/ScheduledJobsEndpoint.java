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

package com.proit.app.jobs.endpoint;

import com.proit.app.jobs.model.dto.JobTrigger;
import com.proit.app.jobs.model.dto.RescheduleJobRequest;
import com.proit.app.jobs.model.dto.param.JobParams;
import com.proit.app.jobs.service.JobManager;
import com.proit.app.api.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@Tag(name = "scheduled-jobs-endpoint", description = "Методы для работы с периодическими задачами внутри сервиса.")
@RestController
@RequestMapping("/api/scheduledJobs")
@RequiredArgsConstructor
public class ScheduledJobsEndpoint
{
	private final JobManager jobManager;

	@GetMapping("/list")
	@Operation(summary = "Возвращает список всех периодических задач и их описание.")
	public ApiResponse<Map<String, String>> list()
	{
		return ApiResponse.of(jobManager.getJobList());
	}

	@PostMapping("/execute")
	@Deprecated(forRemoval = true)
	@Operation(summary = "Позволяет выполнить асинхронно указанную периодическую задачу, не дожидаясь планового расписания.")
	public ApiResponse<?> executeForRemoval(@RequestParam String jobName)
	{
		jobManager.executeJob(jobName, null);

		return ApiResponse.ok();
	}

	@PostMapping("/reschedule")
	@Deprecated(forRemoval = true)
	@Operation(summary = "Позволяет изменить план расписания периодической задачи.")
	public ApiResponse<?> rescheduleJobForRemoval(@RequestBody @Valid RescheduleJobRequest request)
	{
		jobManager.rescheduleJob(request.getJobName(), request.getTriggerType().createTrigger(request.getExpression()));

		return ApiResponse.ok();
	}

	@PostMapping("/{jobName}/execute")
	@Operation(summary = "Позволяет выполнить асинхронно указанную периодическую задачу, изменив значение параметров задачи.")
	public ApiResponse<?> execute(@PathVariable String jobName, @RequestBody(required = false) Map<String, Object> params)
	{
		jobManager.executeJob(jobName, params);

		return ApiResponse.ok();
	}

	@PostMapping("/{jobName}/reschedule")
	@Operation(summary = "Позволяет изменить план расписания периодической задачи.")
	public ApiResponse<?> rescheduleJob(@RequestBody @Valid RescheduleJobRequest request,
	                                    @PathVariable String jobName)
	{
		jobManager.rescheduleJob(jobName, request.getTriggerType().createTrigger(request.getExpression()));

		return ApiResponse.ok();
	}

	@GetMapping("/{jobName}/trigger")
	@Operation(summary = "Возвращает план расписания для указанной задачи.")
	public ApiResponse<JobTrigger> getTrigger(@PathVariable String jobName)
	{
		return ApiResponse.of(jobManager.getTrigger(jobName));
	}

	@GetMapping("/{jobName}/params")
	@Operation(summary = "Возвращает схему параметров для указанной задачи.")
	public ApiResponse<JobParams> getParams(@PathVariable String jobName)
	{
		return ApiResponse.of(jobManager.getParams(jobName));
	}
}
