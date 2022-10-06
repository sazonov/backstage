/*
 *    Copyright 2019-2022 the original author or authors.
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

package com.proit.app.service.attachment;

import com.proit.app.model.domain.Attachment;
import com.proit.app.model.other.JobResult;
import com.proit.app.repository.AttachmentRepository;
import com.proit.app.service.attachment.store.AttachmentStore;
import com.proit.app.service.job.AbstractFixedDelayJob;
import com.proit.app.service.job.JobDescription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty("app.attachments.deleteUnbounded")
@JobDescription("Удаление мусорных вложений, для которых нет связей с объектами системы")
@RequiredArgsConstructor
public class DeleteUnboundedAttachmentsJob extends AbstractFixedDelayJob
{
	private final AttachmentRepository attachmentRepository;
	private final AttachmentStore attachmentStore;

	@Override
	public long getFixedDelay()
	{
		return 60 * 60 * 1000;
	}

	@Override
	protected JobResult execute()
	{
		log.info("Deleting unbounded attachments...");

		Date expirationDate = new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24);

		List<Attachment> expiredAttachments = attachmentRepository.findExpiredUnbounded(expirationDate, PageRequest.of(0, 500));

		for (Attachment attachment : expiredAttachments)
		{
			try
			{
				attachmentStore.deleteAttachment(attachment);

				attachmentRepository.delete(attachment);
			}
			catch (Exception e)
			{
				log.info(String.format("Failed to delete unbounded attachment '%s'.", attachment.getId()), e);
			}
		}

		if (!expiredAttachments.isEmpty())
		{
			log.info("'{}' unbounded attachment(s) processed.", expiredAttachments.size());
		}

		return JobResult.ok();
	}
}
