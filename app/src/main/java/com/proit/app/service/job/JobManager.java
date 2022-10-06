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

package com.proit.app.service.job;

import com.proit.app.exception.ObjectNotFoundException;
import com.proit.app.model.other.JobResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobManager
{
	private final TaskExecutor taskExecutor;

	private final Map<String, AbstractJob> jobs;

	public Map<String, String> getJobList()
	{
		return jobs.entrySet().stream()
				.map(this::getJobDescription)
				.sorted(Map.Entry.comparingByKey())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o, n) -> n, LinkedHashMap::new));
	}

	public JobResult executeJobAndWait(Class<? extends AbstractJob> jobClass)
	{
		log.info("Executing scheduled job by class name '{}' manually.", jobClass.getSimpleName());

		var job = jobs.values().stream().filter(jobClass::isInstance).findFirst().orElseThrow(() -> new ObjectNotFoundException(AbstractJob.class, jobClass.getSimpleName()));

		return job.execute();
	}

	public void executeJob(String jobName)
	{
		log.info("Executing scheduled job '{}' manually.", jobName);

		if (!jobs.containsKey(jobName))
		{
			throw new ObjectNotFoundException(AbstractJob.class, jobName);
		}

		taskExecutor.execute(jobs.get(jobName));
	}

	private Map.Entry<String, String> getJobDescription(Map.Entry<String, AbstractJob> entry)
	{
		var annotation = AnnotationUtils.findAnnotation(entry.getValue().getClass(), JobDescription.class);
		var value = Optional.ofNullable(annotation).map(JobDescription::value).orElse("");

		return Map.entry(entry.getKey(), value);
	}
}
