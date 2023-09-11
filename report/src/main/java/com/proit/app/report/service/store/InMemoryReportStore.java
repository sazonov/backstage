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

package com.proit.app.report.service.store;

import com.proit.app.report.configuration.properties.ReportProperties;
import com.proit.app.report.model.report.Report;
import com.proit.app.report.model.report.SimpleReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(value = ReportProperties.STORE_TYPE_PROPERTY, havingValue = ReportProperties.STORE_TYPE_MEMORY, matchIfMissing = true)
public class InMemoryReportStore implements ReportStore
{
	private final Map<String, SimpleReport> reportMap = new ConcurrentHashMap<>();
	private final Map<String, String> reportLinkObjectMap = new ConcurrentHashMap<>();

	@Override
	public Report create(String fileName, String mimeType, byte[] data)
	{
		var report = SimpleReport.builder()
				.id(UUID.randomUUID().toString())
				.fileName(fileName)
				.data(data)
				.size(data.length)
				.build();

		reportMap.put(report.getId(), report);

		return report;
	}

	@Override
	public Report getById(String id)
	{
		return reportMap.get(id);
	}

	@Override
	public String getIdByLinkedObject(String objectKey, String objectId)
	{
		return reportLinkObjectMap.get(objectId);
	}

	@Override
	public void linkReportWithObject(String id, String objectKey, String objectId)
	{
		reportLinkObjectMap.put(objectId, id);
	}

	@Override
	public Report update(String id, String fileName, String mimeType, byte[] data)
	{
		return reportMap.computeIfPresent(id, (reportId, report) -> {
			report.setData(data);
			report.setSize(data.length);

			return report;
		});
	}

	@Override
	public void release(String objectKey, String objectId)
	{
		var reportId = reportLinkObjectMap.remove(objectId);

		reportMap.remove(reportId);
	}
}
