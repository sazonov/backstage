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

package com.proit.bpm.service.job;

import com.proit.app.model.other.JobResult;
import com.proit.app.service.job.AbstractCronJob;
import com.proit.app.service.job.JobDescription;
import com.proit.bpm.model.Workflow;
import com.proit.bpm.repository.DeployedWorkflowRepository;
import com.proit.bpm.repository.ProcessRepository;
import com.proit.bpm.service.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@JobDescription("Архивация устаревших ревизий схем процессов")
@RequiredArgsConstructor
public class ArchiveWorkflowsJob extends AbstractCronJob
{
	private final ProcessRepository processRepository;
	private final DeployedWorkflowRepository deployedWorkflowRepository;

	private final WorkflowService workflowService;

	public String getCronExpression()
	{
		return "0 0 1 * * *";
	}

	public JobResult execute()
	{
		log.info("Archiving workflows.");

		var actualWorkflowsIds = workflowService.getWorkflows().stream().collect(Collectors.groupingBy(Workflow::getId,
				Collectors.maxBy(Comparator.comparing(Workflow::getVersion)))).values().stream()
				.filter(Optional::isPresent).map(Optional::get).map(Workflow::getFullId).toList();

		var activeWorkflowIds = processRepository.findActiveWorkflowIds();

		log.info("Actual workflows: {}.", actualWorkflowsIds);
		log.info("Active workflows: {}.", activeWorkflowIds);

		for (var workflowId : deployedWorkflowRepository.findAllIdsByArchivedIsNull())
		{
			log.info("Checking workflow {}.", workflowId);

			if (!actualWorkflowsIds.contains(workflowId) && !activeWorkflowIds.contains(workflowId))
			{
				workflowService.archiveWorkflow(workflowId);
			}
		}

		return JobResult.ok();
	}
}
