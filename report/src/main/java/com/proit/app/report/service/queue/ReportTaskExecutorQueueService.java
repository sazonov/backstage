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

package com.proit.app.report.service.queue;

import com.proit.app.report.model.ReportMessage;
import com.proit.app.report.service.ReportService;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class ReportTaskExecutorQueueService implements ReportQueueService
{
	private final TaskExecutor taskExecutor;

	private final ReportService reportService;

	public ReportTaskExecutorQueueService(TaskExecutor taskExecutor, @Lazy ReportService reportService)
	{
		this.taskExecutor = taskExecutor;
		this.reportService = reportService;
	}

	@Override
	public void executeReportRequestMessage(ReportMessage message)
	{
		taskExecutor.execute(() -> reportService.run(message));
	}

	@Override
	public void executeReportGeneratingMessage(ReportMessage message)
	{
		taskExecutor.execute(() -> reportService.generate(message));
	}
}
