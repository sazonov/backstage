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

package com.proit.app.attachment.service;

import com.proit.app.attachment.repository.AttachmentRepository;
import com.proit.app.jobs.model.dto.other.JobResult;
import com.proit.app.jobs.model.dto.param.EmptyJobParams;
import com.proit.app.jobs.service.AbstractFixedDelayJob;
import com.proit.app.jobs.service.JobDescription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty("app.attachments.deleteUnbounded")
@JobDescription("Удаление мусорных вложений, для которых нет связей с объектами системы")
@RequiredArgsConstructor
public class DeleteUnboundedAttachmentsJob extends AbstractFixedDelayJob<EmptyJobParams>
{
	private final AttachmentRepository attachmentRepository;
	private final AttachmentService attachmentService;

	@Override
	public long getFixedDelay()
	{
		return 60 * 60 * 1000;
	}

	@Override
	protected JobResult execute()
	{
		log.info("Deleting unbounded attachments...");

		var expirationDate = LocalDateTime.now().minusDays(1);

		var expiredAttachmentIds = attachmentRepository.findExpiredUnbounded(expirationDate, PageRequest.of(0, 500));

		for (var attachmentId : expiredAttachmentIds)
		{
			try
			{
				attachmentService.deleteAttachment(attachmentId);
			}
			catch (Exception e)
			{
				log.info(String.format("Failed to delete unbounded attachment '%s'.", attachmentId), e);
			}
		}

		if (!expiredAttachmentIds.isEmpty())
		{
			log.info("'{}' unbounded attachment(s) processed.", expiredAttachmentIds.size());
		}

		return JobResult.ok(Map.of("processed", expiredAttachmentIds.size()));
	}
}
