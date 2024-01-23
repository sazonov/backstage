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

package com.proit.app.quartz.configuration;

import com.proit.app.quartz.configuration.properties.QuartzProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;

@Configuration
@DependsOn("DDLConfiguration")
@RequiredArgsConstructor
@EnableConfigurationProperties(QuartzProperties.class)
@ConditionalOnProperty(QuartzProperties.ACTIVATION_PROPERTY)
public class QuartzConfiguration
{
	private final QuartzProperties quartzProperties;

	@Value(value = "classpath:/quartz.properties")
	private Resource schedulerProperties;

	@Bean
	public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource, PlatformTransactionManager transactionManager, JobFactory jobFactory) throws IOException
	{
		SchedulerFactoryBean factory = new SchedulerFactoryBean();

		Properties properties = PropertiesLoaderUtils.loadProperties(schedulerProperties);

		if (quartzProperties.isOnlyForScheduling())
		{
			properties.put("org.quartz.threadPool.class", "com.proit.app.quartz.configuration.ZeroSizeThreadPool");
		}

		if (StringUtils.isNotBlank(quartzProperties.getScheme()))
		{
			properties.put("org.quartz.jobStore.tablePrefix", "%s.qrtz_".formatted(quartzProperties.getScheme()));
		}

		if (quartzProperties.isClustered())
		{
			properties.put("org.quartz.jobStore.isClustered", "true");
			properties.put("org.quartz.scheduler.instanceId", "AUTO");
			properties.put("org.quartz.jobStore.clusterCheckinInterval", "20000");
		}
		else
		{
			properties.put("org.quartz.jobStore.acquireTriggersWithinLock", "false");
			properties.put("org.quartz.jobStore.lockHandler.class", "org.quartz.impl.jdbcjobstore.SimpleSemaphore");
		}

		properties.put("org.quartz.scheduler.instanceName", quartzProperties.getSchedulerName());
		properties.put("org.quartz.threadPool.threadCount", quartzProperties.getThreadCount().toString());

		factory.setQuartzProperties(properties);
		factory.setDataSource(dataSource);
		factory.setJobFactory(jobFactory);
		factory.setAutoStartup(true);
		factory.setWaitForJobsToCompleteOnShutdown(true);
		factory.setTransactionManager(transactionManager);

		return factory;
	}

	@Bean
	public JobFactory jobFactory(ApplicationContext context)
	{
		AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
		jobFactory.setApplicationContext(context);

		return jobFactory;
	}
}
