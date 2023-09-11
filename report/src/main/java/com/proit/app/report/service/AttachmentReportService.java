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

import com.proit.app.report.model.*;
import com.proit.app.report.service.store.AttachmentReportStore;
import com.proit.app.report.service.store.ReportStore;
import com.proit.app.utils.transactional.TransactionalExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class AttachmentReportService extends AbstractReportService
{
	private final AttachmentReportStore reportStore;
	private final TransactionalExecutor transactionalExecutor;

	@Override
	public ReportStore getReportStore()
	{
		return reportStore;
	}

	@Transactional
	public void run(ReportMessage message)
	{
		try
		{
			transactionalExecutor.execute(() -> runInternal(message));
		}
		catch (Exception e)
		{
			var reportType = message.getReportFilter()
					.getReportType();

			log.error("Ошибка генерации отчета {}.", reportType, e);

			transactionalExecutor.execute(() -> updateReportTask(message.getTaskId(), false, null, null));
		}
	}

	@Transactional
	public void generate(ReportMessage message)
	{
		var reportType = message.getReportFilter()
				.getReportType();

		log.info("Начало генерации отчета {}.", reportType);

		try
		{
			byte[] data = generatorLocator.getGenerator(message.getReportFilter().getReportType())
					.generate(message.getReportFilter());

			transactionalExecutor.execute(() -> updateReportTask(message.getTaskId(), true, data, reportType.getReportFileType()));
		}
		catch (Exception e)
		{
			log.error("Ошибка генерации отчета {}.", reportType, e);

			transactionalExecutor.execute(() -> updateReportTask(message.getTaskId(), false, null, reportType.getReportFileType()));
		}

		log.info("Завершение генерации отчета {}.", reportType);
	}
}

