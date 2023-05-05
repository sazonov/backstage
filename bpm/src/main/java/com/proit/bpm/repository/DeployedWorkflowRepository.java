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

package com.proit.bpm.repository;

import com.proit.app.repository.generic.CustomJpaRepository;
import com.proit.bpm.domain.DeployedWorkflow;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DeployedWorkflowRepository extends CustomJpaRepository<DeployedWorkflow, String>
{
	@Query("select w.id from DeployedWorkflow w where w.archived is null")
	List<String> findAllIdsByArchivedIsNull();

	List<DeployedWorkflow> findAllByArchivedIsNull();

	List<DeployedWorkflow> findAllByChecksumIsNull();

	Optional<DeployedWorkflow> findFirstByChecksum(String checksum);
}
