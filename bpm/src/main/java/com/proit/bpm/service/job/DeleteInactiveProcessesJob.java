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

package com.proit.bpm.service.job;

import com.proit.app.jobs.model.dto.other.JobResult;
import com.proit.app.jobs.model.dto.param.EmptyJobParams;
import com.proit.app.jobs.service.AbstractCronJob;
import com.proit.app.jobs.service.JobDescription;
import com.proit.bpm.configuration.properties.BpmProperties;
import com.proit.bpm.repository.ProcessRepository;
import com.proit.bpm.service.process.ProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty("app.bpm.delete-inactive-processes-delay")
@JobDescription("Удаляет неактивные (остановленные) процессы после истечения указанного в конфиге времени")
@RequiredArgsConstructor
public class DeleteInactiveProcessesJob extends AbstractCronJob<EmptyJobParams>
{
	private final ProcessRepository processRepository;

	private final ProcessService processService;

	private final BpmProperties bpmProperties;

	public String getCronExpression()
	{
		return "0 0 2 * * *";
	}

	public JobResult execute()
	{
		var stoppedBefore = Timestamp.valueOf(LocalDateTime.now().minus(bpmProperties.getDeleteInactiveProcessesDelay()));

		log.info("Deleting processes stopped before '{}'.", stoppedBefore);

		long deletedProcessCount = 0;
		long errorCount = 0;

		for (var processId : processRepository.findProcessIdsByActiveIsFalse(stoppedBefore))
		{
			try
			{
				log.info("Deleting process '{}'.", processId);

				processService.deleteProcess(processId);

				deletedProcessCount++;
			}
			catch (Exception e)
			{
				log.warn(String.format("Failed to delete inactive process '%s'.", processId), e);

				errorCount++;
			}
		}

		return JobResult.ok(Map.of(
				"deletedProcessCount", deletedProcessCount,
				"errorCount", errorCount
		));
	}
}
