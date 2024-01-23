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

import com.proit.app.database.repository.CustomJpaRepository;
import com.proit.bpm.domain.Task;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface TaskRepository extends CustomJpaRepository<Task, String>, JpaSpecificationExecutor<Task>
{
	Task findByWorkItemId(Long workItemId);

	List<Task> findAllByProcessId(String processId);

	@Modifying
	void deleteByProcessId(String processId);
}
