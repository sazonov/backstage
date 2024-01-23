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

import com.proit.app.attachment.model.domain.Attachment;
import com.proit.app.attachment.service.AttachmentService;
import com.proit.app.exception.ObjectNotFoundException;
import com.proit.app.report.configuration.properties.ReportProperties;
import com.proit.app.report.model.report.Report;
import com.proit.app.report.model.report.SimpleReport;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = ReportProperties.STORE_TYPE_PROPERTY, havingValue = ReportProperties.STORE_TYPE_ATTACHMENT)
public class AttachmentReportStore implements ReportStore
{
	private static final String USER_ID = "-1";

	private final AttachmentService attachmentService;

	@Override
	public Report create(String fileName, String mimeType, byte[] data)
	{
		var attachment = attachmentService.addAttachment(fileName, mimeType, USER_ID, data);

		return buildReportAttachment(attachment);
	}

	@Override
	public Report getById(String id)
	{
		var attachment = attachmentService.getAttachment(id);

		return buildReportAttachment(attachment);
	}

	@Override
	public String getIdByLinkedObject(String objectKey, String objectId)
	{
		return attachmentService.getAttachmentIds(objectKey, objectId)
				.stream()
				.findFirst()
				.orElseThrow(() -> new ObjectNotFoundException(Attachment.class, objectId));
	}

	@Override
	public void linkReportWithObject(String id, String objectKey, String objectId)
	{
		attachmentService.bindAttachment(id, USER_ID, objectKey, objectId);
	}

	@Override
	public Report update(String id, String fileName, String mimeType, byte[] data)
	{
		var attachment = attachmentService.updateAttachment(id, fileName, mimeType, USER_ID, data);

		return buildReportAttachment(attachment);
	}

	@Override
	public void release(String objectKey, String objectId)
	{
		attachmentService.releaseAttachments(objectKey, objectId);
	}

	private Report buildReportAttachment(Attachment attachment)
	{
		return SimpleReport.builder()
				.id(attachment.getId())
				.size(attachment.getSize())
				.build();
	}
}
