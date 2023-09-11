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

package com.proit.app.report.service.task;

import com.proit.app.report.model.filter.ReportFilter;
import com.proit.app.report.model.ReportStatus;
import com.proit.app.report.model.task.ReportTask;

public interface ReportTaskAdvice
{
	default void handleGet(String id)
	{
	}

	default void handleCreate(String userId, ReportFilter reportFilter, String reportFileName)
	{
	}

	default void handleBeforeComplete(String id, ReportStatus reportStatus, String reportId)
	{
	}

	default void handleAfterComplete(ReportTask reportTask)
	{
	}
}
