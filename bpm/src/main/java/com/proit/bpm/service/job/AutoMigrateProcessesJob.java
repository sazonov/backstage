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

package com.proit.bpm.service.job;

import com.proit.app.model.dto.job.EmptyJobParams;
import com.proit.app.model.other.JobResult;
import com.proit.app.service.job.AbstractCronJob;
import com.proit.app.service.job.JobDescription;
import com.proit.bpm.model.Workflow;
import com.proit.bpm.repository.ProcessRepository;
import com.proit.bpm.service.ProcessService;
import com.proit.bpm.service.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnProperty("app.bpm.auto-migrate-processes")
@JobDescription("Автоматическая миграция запущенных процессов на самую последнюю ревизию схемы")
@RequiredArgsConstructor
public class AutoMigrateProcessesJob extends AbstractCronJob<EmptyJobParams>
{
	private final ProcessRepository processRepository;

	private final WorkflowService workflowService;
	private final ProcessService processService;

	public String getCronExpression()
	{
		return "0 0 3 * * *";
	}

	public JobResult execute()
	{
		log.info("Migrating processes to latest revisions.");

		var workflowRevisions = workflowService.getWorkflows().stream().collect(Collectors.groupingBy(Workflow::getId));
		var actualWorkflows = workflowService.getWorkflows().stream().collect(Collectors.groupingBy(Workflow::getId,
				Collectors.maxBy(Comparator.comparing(Workflow::getVersion))));

		long migratedProcessCount = 0;
		long errorCount = 0;

		for (var workflowId : actualWorkflows.keySet())
		{
			var oldRevisions = workflowRevisions.get(workflowId).stream().filter(it -> !it.equals(actualWorkflows.get(workflowId).get()))
					.map(Workflow::getFullId).collect(Collectors.toList());

			if (oldRevisions.isEmpty())
			{
				continue;
			}

			for (var processId : processRepository.findProcessIdsByWorkflowIds(oldRevisions))
			{
				try
				{
					processService.migrateProcess(processId, workflowId);

					migratedProcessCount++;
				}
				catch (Exception e)
				{
					log.warn(String.format("Failed to migrate process '%s' to latest workflow.", processId), e);

					errorCount++;
				}
			}
		}

		log.info("Migrating processes to latest revisions completed.");

		return JobResult.ok(Map.of(
				"migratedProcessCount", migratedProcessCount,
				"errorCount", errorCount
		));
	}
}
