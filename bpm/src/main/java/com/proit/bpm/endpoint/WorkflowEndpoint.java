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

package com.proit.bpm.endpoint;

import com.proit.app.model.api.ApiResponse;
import com.proit.app.model.api.OkResponse;
import com.proit.bpm.conversion.dto.WorkflowConverter;
import com.proit.bpm.model.dto.DeployWorkflowRequest;
import com.proit.bpm.model.dto.WorkflowDto;
import com.proit.bpm.service.workflow.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(value = "/api/bpm/workflow", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "workflow-endpoint", description = "Методы для работы с описаниями бизнес-процессов.")
@RequiredArgsConstructor
public class WorkflowEndpoint
{
	private final WorkflowService workflowService;

	private final WorkflowConverter workflowDetailsConverter;

	@PostMapping("/deploy")
	@Operation(summary = "Устанавливает бизнес-процесс. Автоматически инкрементирует версию, если такой процесс уже установлен.")
	public OkResponse deployWorkflow(@Valid @RequestBody DeployWorkflowRequest request)
	{
		workflowService.deployWorkflow(request.getId(), request.getVersion(), request.getDefinition(), request.isForce());

		return ApiResponse.ok();
	}

	@GetMapping("/list")
	@Operation(summary = "Возвращает список всех загруженных процессов.")
	public ApiResponse<List<WorkflowDto>> workflowList()
	{
		return ApiResponse.of(workflowDetailsConverter.convert(workflowService.getWorkflows()));
	}

	@PostMapping("/reload")
	@Operation(summary = "Перезагружает все доступные схемы бизнес-процессов и возвращает информацию о них.")
	public ApiResponse<List<WorkflowDto>> reloadWorkflowList()
	{
		workflowService.initialize();

		return workflowList();
	}
}
