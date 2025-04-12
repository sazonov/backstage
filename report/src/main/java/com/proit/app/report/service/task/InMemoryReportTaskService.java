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

package com.proit.app.report.service.task;

import com.proit.app.report.model.ReportStatus;
import com.proit.app.report.model.filter.ReportFilter;
import com.proit.app.report.model.task.ReportTask;
import com.proit.app.report.model.task.SimpleReportTask;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryReportTaskService implements ReportTaskService
{
	private final Map<String, SimpleReportTask> reportTaskMap = new ConcurrentHashMap<>();

	@Override
	public ReportTask getById(String id)
	{
		return reportTaskMap.get(id);
	}

	@Override
	public ReportTask create(String userId, ReportFilter reportFilter, String reportFileName)
	{
		var task = SimpleReportTask.builder()
				.id(UUID.randomUUID().toString())
				.userId(userId)
				.reportStatus(ReportStatus.GENERATING)
				.reportFilter(reportFilter)
				.build();

		reportTaskMap.put(task.getId(), task);

		return task;
	}

	/**
	 * Удаляем ReportTask т.к. в случае с inMemory решением нет необходимости
	 * его обновлять и хранить после завершения генерации отчета.
	 * */
	@Override
	public ReportTask complete(String id, ReportStatus reportStatus, String reportId)
	{
		var task = reportTaskMap.remove(id);
		task.setReportStatus(reportStatus);
		task.setReportId(reportId);

		return task;
	}

	@Override
	public List<ReportTask> getGeneratingReportTasks()
	{
		throw new UnsupportedOperationException();
	}
}
