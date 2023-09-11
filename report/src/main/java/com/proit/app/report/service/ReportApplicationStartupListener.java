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

package com.proit.app.report.service;

import com.proit.app.report.model.ReportMessage;
import com.proit.app.report.model.task.ReportTask;
import com.proit.app.report.service.queue.ReportQueueService;
import com.proit.app.report.service.task.InMemoryReportTaskService;
import com.proit.app.report.service.task.ReportTaskManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnMissingBean(InMemoryReportTaskService.class)
public class ReportApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent>
{
	private final ReportTaskManager reportTaskManager;
	private final ReportQueueService reportQueueService;

	@Override
	public void onApplicationEvent(@NotNull final ApplicationReadyEvent event)
	{
		var rowTasks = Optional.ofNullable(reportTaskManager.getGeneratingReportTasks())
				.orElseGet(List::of);

		if (rowTasks.isEmpty())
		{
			log.info("Необработанные задачи отсутствуют.");

			return;
		}

		log.info("Будет обработано задач: {}.", rowTasks.size());

		rowTasks.stream()
				.map(this::buildReportMessage)
				.forEach(reportQueueService::executeReportGeneratingMessage);
	}

	private ReportMessage buildReportMessage(ReportTask reportTask)
	{
		var reportMessage = new ReportMessage();

		reportMessage.setReportFilter(reportTask.getReportFilter());
		reportMessage.setTaskId(reportTask.getId());
		reportMessage.setUserId(reportTask.getUserId());

		return reportMessage;
	}
}
