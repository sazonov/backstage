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

package com.proit.bpm.service;

import com.proit.bpm.domain.Process;
import com.proit.bpm.domain.TaskResult;
import com.proit.bpm.model.ProcessTimer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProcessEngine
{
	Process startProcess(String workflowId, Map<String, Object> parameters);

	Optional<Process> startProcessOnEvent(String event, Map<String, Object> eventParameters, Map<String, Object> processParameters);

	void stopProcess(String processId);

	void deleteProcess(String processId);

	void killProcess(String processId);

	void completeTask(String taskId, TaskResult taskResult);

	void cancelTask(String taskId);

	void sendEvent(String processId, String event, Map<String, Object> eventParameters);

	void updateProcessParameters(String processId, Map<String, Object> parameters);

	void migrateProcess(String processId, String workflowId);

	List<ProcessTimer> getProcessTimers(String processId);

	void updateProcessTimer(String processId, String timerName, LocalDateTime nextFireTime);
}
