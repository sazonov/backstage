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

package com.proit.app.jobs.configuration;

import com.proit.app.jobs.configuration.properties.SchedulerProperties;
import com.proit.app.jobs.service.AbstractJob;
import com.proit.app.jobs.service.JobHealthIndicator;
import com.proit.app.utils.ActuatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@EnableScheduling
@Configuration
@EnableConfigurationProperties(SchedulerProperties.class)
@ConditionalOnProperty(value = SchedulerProperties.ACTIVATION_PROPERTY, matchIfMissing = true)
@RequiredArgsConstructor
public class SchedulerConfiguration implements SchedulingConfigurer
{
	private final SchedulerProperties schedulerProperties;

	@Bean
	public ThreadPoolTaskScheduler taskScheduler()
	{
		var sch = new ThreadPoolTaskScheduler();
		sch.setPoolSize(schedulerProperties.getPoolSize());

		return sch;
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar)
	{
		taskRegistrar.setScheduler(taskScheduler());
	}

	@Bean
	public HealthContributor scheduledJobsHealthContributor(Map<String, AbstractJob> jobs)
	{
		log.info("Scheduled jobs found: {}.", jobs.keySet());

		var healthIndicators = jobs.entrySet()
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey, it -> new JobHealthIndicator(it.getValue())));

		return ActuatorUtils.createHealthComposite(healthIndicators);
	}
}
