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

import com.proit.app.attachment.configuration.properties.AttachmentProperties;
import com.proit.app.exception.AppException;
import com.proit.app.model.other.exception.ApiStatusCodeImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.attachments.check-mime-types")
@RequiredArgsConstructor
public class AttachmentMimeTypeValidator implements AttachmentServiceAdvice
{
	private final AttachmentProperties attachmentProperties;

	@Override
	public void handleAddAttachment(String id, String fileName, String mimeType, String userId, byte[] data)
	{
		if (!attachmentProperties.getMimeTypes().contains(mimeType.toLowerCase()))
		{
			throw new AppException(ApiStatusCodeImpl.ATTACHMENT_TYPE_NOT_SUPPORTED);
		}
	}
}
