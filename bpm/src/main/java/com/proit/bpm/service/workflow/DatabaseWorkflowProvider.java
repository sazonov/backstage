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

import com.proit.bpm.model.Workflow;
import com.proit.bpm.repository.DeployedWorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.util.Version;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Загружает процессы, находящиеся в таблице workflow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseWorkflowProvider extends AbstractWorkflowProvider
{
	private final DeployedWorkflowRepository deployedWorkflowRepository;

	private final TransactionOperations transactionOperations;

	@PostConstruct
	public void calculateMissingChecksums()
	{
		transactionOperations.executeWithoutResult((status) -> {
			deployedWorkflowRepository.findAllByChecksumIsNull().forEach(workflow -> {
				log.info("Calculating workflow checksum: {}.", workflow.getId());

				workflow.setChecksum(DigestUtils.md5Hex(workflow.getDefinition()));
			});
		});
	}

	public List<Workflow> getWorkflows()
	{
		return deployedWorkflowRepository.findAllByArchivedIsNull().stream().map(it -> {
			String[] idVersion = it.getId().split("_");

			Workflow details = new Workflow();
			details.setId(idVersion[0]);
			details.setVersion(Version.parse(idVersion[1]));
			details.setCreated(it.getCreated());
			details.setDefinition(it.getDefinition());

			return details;
		}).peek(this::processWorkflow).collect(Collectors.toList());
	}
}
