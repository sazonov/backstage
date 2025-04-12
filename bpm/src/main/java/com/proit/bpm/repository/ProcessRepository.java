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

package com.proit.bpm.repository;

import com.proit.app.database.repository.CustomJpaRepository;
import com.proit.bpm.domain.Process;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface ProcessRepository extends CustomJpaRepository<Process, String>, JpaSpecificationExecutor<Process>
{
	Optional<Process> findByInstanceId(String instanceId);

	@Query(nativeQuery = true, value = "select * from process p where p.active = ?1 and p.parameters @> jsonb_build_object(?2, ?3)")
	List<Process> findByParameters(Boolean isActive, String paramName1, String paramValue1);

	@Query("select p.id from Process p where p.workflowId in :workflowIds and p.active = true")
	List<String> findProcessIdsByWorkflowIds(List<String> workflowIds);

	@Query("select distinct p.workflowId from Process p where p.active = true")
	List<String> findActiveWorkflowIds();

	@Query("select p.id from Process p where p.active = false and p.version <= :updatedBefore")
	List<String> findProcessIdsByActiveIsFalse(@Param("updatedBefore") Timestamp updatedBefore);
}
