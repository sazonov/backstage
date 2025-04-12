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

package com.proit.app.report.configuration;

import com.proit.app.report.configuration.properties.ReportProperties;
import com.proit.app.report.service.AttachmentReportService;
import com.proit.app.report.service.InMemoryReportService;
import com.proit.app.report.service.ReportService;
import com.proit.app.report.service.store.AttachmentReportStore;
import com.proit.app.report.service.store.InMemoryReportStore;
import com.proit.app.utils.transactional.TransactionalExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ReportProperties.class)
@ConditionalOnProperty(value = ReportProperties.ACTIVATION_PROPERTY, matchIfMissing = true)
public class ReportConfiguration
{
	@Bean
	@ConditionalOnProperty(value = ReportProperties.STORE_TYPE_PROPERTY, havingValue = ReportProperties.STORE_TYPE_ATTACHMENT)
	public ReportService attachmentReportService(AttachmentReportStore attachmentReportStore, TransactionalExecutor transactionalExecutor)
	{
		return new AttachmentReportService(attachmentReportStore, transactionalExecutor);
	}

	@Bean
	@ConditionalOnProperty(value = ReportProperties.STORE_TYPE_PROPERTY, havingValue = ReportProperties.STORE_TYPE_MEMORY, matchIfMissing = true)
	public ReportService inMemoryReportService(InMemoryReportStore inMemoryReportStore)
	{
		return new InMemoryReportService(inMemoryReportStore);
	}
}
