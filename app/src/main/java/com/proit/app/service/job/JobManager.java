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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@DependsOn("healthContributor")
public class JobManager
{
	private final ThreadPoolTaskScheduler taskScheduler;

	@Getter
	private final Map<String, AbstractJob> jobs;

	@PostConstruct
	public void scheduledJobs()
	{
		jobs.values().forEach(this::scheduleJob);
	}

	public Map<String, String> getJobList()
	{
		return jobs.entrySet().stream()
				.map(this::getJobDescription)
				.sorted(Map.Entry.comparingByKey())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o, n) -> n, LinkedHashMap::new));
	}

	private Map.Entry<String, String> getJobDescription(Map.Entry<String, AbstractJob> entry)
	{
		var annotation = AnnotationUtils.findAnnotation(entry.getValue().getClass(), JobDescription.class);
		var value = Optional.ofNullable(annotation).map(JobDescription::value).orElse("");

		return Map.entry(entry.getKey(), value);
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

		taskScheduler.execute(jobs.get(jobName));
	}

	public void rescheduleJob(String jobName, Trigger trigger)
	{
		var job = jobs.get(jobName);

		if (job == null)
		{
			throw new ObjectNotFoundException(AbstractJob.class, jobName);
		}

		job.cancel();
		job.setTrigger(trigger);

		scheduleJob(job);
	}

	private void scheduleJob(AbstractJob job)
	{
		job.beforeScheduled();

		job.setFuture(taskScheduler.schedule(job, job.getTrigger()));

		job.afterScheduled();
	}
}
