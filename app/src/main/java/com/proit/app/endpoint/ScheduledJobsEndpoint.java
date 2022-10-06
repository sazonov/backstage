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

package com.proit.app.endpoint;

import com.proit.app.model.api.ApiResponse;
import com.proit.app.service.job.JobManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
	@Operation(summary = "Позволяет выполнить асинхронно указанную периодическую задачу, не дожидаясь планового расписания.")
	public ApiResponse<?> execute(@RequestParam String jobName)
	{
		jobManager.executeJob(jobName);

		return ApiResponse.ok();
	}
}
