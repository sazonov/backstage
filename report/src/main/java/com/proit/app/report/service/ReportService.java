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

import com.proit.app.report.model.filter.ReportFilter;
import com.proit.app.report.model.ReportMessage;
import com.proit.app.report.model.ReportType;
import com.proit.app.report.model.task.ReportTask;
import org.springframework.util.concurrent.ListenableFuture;

public interface ReportService
{
	ReportTask generate(ReportType type);

	ReportTask generate(ReportFilter filter);

	ReportTask generate(ReportType type, String userId);

	ReportTask generate(ReportFilter filter, String userId);

	ListenableFuture<byte[]> generateAsync(ReportType type);

	ListenableFuture<byte[]> generateAsync(ReportFilter filter);

	void generate(ReportMessage message);

	void run(ReportMessage message);
}
