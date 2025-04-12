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

package com.proit.bpm.endpoint;

import com.proit.app.api.model.ApiResponse;
import com.proit.bpm.model.dto.KillProcessRequest;
import com.proit.bpm.model.dto.MigrateProcessRequest;
import com.proit.bpm.model.dto.StartProcessRequest;
import com.proit.bpm.model.dto.StopProcessRequest;
import com.proit.bpm.service.process.ProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping(value = "/api/bpm/process", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "bpm-endpoint", description = "Методы для работы с процессами.")
@RequiredArgsConstructor
public class BpmEndpoint
{
	private final ProcessService processService;

	@PostMapping("/start")
	@Operation(summary = "Запускает процесс. Возвращает идентификатор запущенного инстанса.")
	public ApiResponse<String> startProcess(@Valid @RequestBody StartProcessRequest request)
	{
		return ApiResponse.of(processService.startProcess(request.getWorkflowId(), request.getParameters()).getId());
	}

	@PostMapping("/stop")
	@Operation(summary = "Останавливает процесс.")
	public ApiResponse<?> stopProcess(@Valid @RequestBody StopProcessRequest request)
	{
		processService.stopProcess(request.getProcessId());

		return ApiResponse.ok();
	}

	@PostMapping("/kill")
	@Operation(summary = "Убивает процесс.")
	public ApiResponse<?> killProcess(@Valid @RequestBody KillProcessRequest request)
	{
		processService.killProcess(request.getProcessId());

		return ApiResponse.ok();
	}

	@PostMapping("/migrate")
	@Operation(summary = """
			Переводит инстанс процесса на указанный бизнес-процесс.
			Идентификатор процесса задаётся в формате <workflow>_<version>.
			Если версия не указана, то берётся последняя актуальная.
			""")
	public ApiResponse<?> migrateProcess(@Valid @RequestBody MigrateProcessRequest request)
	{
		processService.migrateProcess(request.getProcessId(), request.getWorkflowId());

		return ApiResponse.ok();
	}
}
