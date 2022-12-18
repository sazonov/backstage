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

package com.proit.bpm.service.workflow;

import com.proit.bpm.configuration.properties.BpmProperties;
import com.proit.bpm.domain.DeployedWorkflow;
import com.proit.bpm.exception.BpmException;
import com.proit.bpm.model.Workflow;
import com.proit.bpm.model.WorkflowScript;
import com.proit.bpm.model.event.WorkflowsUpdatedEvent;
import com.proit.bpm.repository.DeployedWorkflowRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.util.Version;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService
{
	private final ApplicationEventPublisher applicationEventPublisher;

	private final DeployedWorkflowRepository deployedWorkflowRepository;

	private final List<WorkflowProvider> workflowProviders;

	private final BpmProperties bpmProperties;

	private final Map<String, Workflow> workflows = new HashMap<>();

	@Getter
	private Set<String> actualWorkflowIds = Set.of();

	private boolean needToReloadWorkflows = false;

	@PostConstruct
	public synchronized void initialize()
	{
		boolean isUpdating = !workflows.isEmpty();

		workflows.clear();

		log.info("Enabled workflow providers: {}.", bpmProperties.getWorkflowProviders());

		workflowProviders.forEach(workflowProvider -> {
			if (bpmProperties.getWorkflowProviders().contains(BpmProperties.WorkflowProviderType.byProviderClass(workflowProvider.getClass())))
			{
				workflowProvider.getWorkflows().forEach(workflow -> {
					log.info("Loading workflow '{}' with version '{}'.", workflow.getId(), workflow.getVersion());

					validateWorkflow(workflow);

					workflows.put(workflow.getFullId(), workflow);
				});
			}
		});

		actualWorkflowIds = workflows.values().stream()
				.collect(Collectors.groupingBy(Workflow::getId, Collectors.maxBy(Comparator.comparing(Workflow::getVersion)))).values().stream()
				.filter(Optional::isPresent).map(Optional::get).map(Workflow::getFullId).collect(Collectors.toSet());

		if (workflows.isEmpty())
		{
			log.warn("No workflow found!");
		}

		if (isUpdating)
		{
			applicationEventPublisher.publishEvent(new WorkflowsUpdatedEvent(this));
		}
	}

	private void validateWorkflow(Workflow workflow)
	{
		if (workflow.getScripts().values().stream().map(WorkflowScript::getType).distinct().count() > 1)
		{
			throw new RuntimeException("failed to validate workflow '%s': more than one script type in one workflow is not supported".formatted(workflow.getFullId()));
		}
	}

	public Collection<Workflow> getWorkflows()
	{
		return workflows.values();
	}

	public String getActualWorkflowId(String workflowId)
	{
		if (workflows.containsKey(workflowId))
		{
			return workflowId;
		}
		else
		{
			return getActualWorkflowIds().stream().filter(it -> it.split("_")[0].equals(workflowId)).findFirst().orElseThrow(() -> {
				throw new BpmException(String.format("workflow '%s' not found", workflowId));
			});
		}
	}

	public WorkflowScript getWorkflowScript(String workflowId, String scriptId)
	{
		if (workflows.containsKey(workflowId))
		{
			return workflows.get(workflowId).getScripts().get(scriptId);
		}

		return null;
	}

	@Scheduled(fixedDelay = 30 * 1000)
	public void checkWorkflows()
	{
		if (needToReloadWorkflows)
		{
			synchronized (this)
			{
				initialize();

				needToReloadWorkflows = false;
			}
		}
	}

	@Transactional
	public synchronized void deployWorkflow(String workflowId, Version version, String definition, boolean force)
	{
		if (!bpmProperties.isDeployEnabled())
		{
			throw new BpmException("deploy is disabled for this app instance");
		}

		var checksum = DigestUtils.md5Hex(definition);
		var existingWorkflow = deployedWorkflowRepository.findFirstByChecksum(checksum);

		if (existingWorkflow.isPresent() && !force)
		{
			var workflow  = existingWorkflow.get();

			log.warn("Workflow is already deployed. Existing workflow id: '{}', checksum: '{}', deploy date: '{}'.", workflow.getId(), workflow.getChecksum(), workflow.getCreated());

			return;
		}

		Optional<Version> maxVersion = deployedWorkflowRepository.findAllIds().stream()
				.filter(it -> it.startsWith(workflowId)).map(it -> Version.parse(it.split("_")[1])).max(Version::compareTo);

		Version targetVersion = version != null ? version : new Version(1, 0);

		if (maxVersion.isPresent())
		{
			if (maxVersion.get().isGreaterThanOrEqualTo(targetVersion))
			{
				targetVersion = new Version(Integer.parseInt(maxVersion.get().toString().split("\\.")[0]) + 1, 0);
			}
		}

		log.info("Deploying workflow '{}' with version '{}'.", workflowId, targetVersion);

		maxVersion.ifPresent(v -> log.info("Previous version of workflow '{}' is '{}'.", workflowId, maxVersion.get()));

		DeployedWorkflow workflow = new DeployedWorkflow();

		workflow.setId(workflowId + "_" + targetVersion);
		workflow.setDefinition(definition);
		workflow.setCreated(LocalDateTime.now());
		workflow.setChecksum(checksum);

		deployedWorkflowRepository.save(workflow);

		needToReloadWorkflows = true;
	}

	@Transactional
	public void archiveWorkflow(String workflowId)
	{
		var workflow = deployedWorkflowRepository.findByIdEx(workflowId);

		workflow.setArchived(LocalDateTime.now());

		log.info("Workflow '{}' archived.", workflow.getId());
	}
}
