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

package com.proit.app.service.attachment.store;

import com.proit.app.configuration.properties.AttachmentProperties;
import com.proit.app.exception.AppException;
import com.proit.app.model.api.ApiStatusCodeImpl;
import com.proit.app.model.domain.Attachment;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public interface AttachmentStore
{
	AttachmentProperties.StoreType getType();

	boolean attachmentExists(Attachment attachment);

	Resource getAttachment(Attachment attachment);

	default void saveAttachment(Attachment attachment, byte[] data)
	{
		saveAttachment(attachment, new ByteArrayInputStream(data));
	}

	void saveAttachment(Attachment attachment, InputStream stream);

	default void saveAttachment(Attachment attachment, Resource resource)
	{
		try
		{
			saveAttachment(attachment, resource.getInputStream());
		}
		catch (IOException e)
		{
			throw new AppException(ApiStatusCodeImpl.ATTACHMENT_ADD_ERROR, e);
		}
	}

	void deleteAttachment(Attachment attachment);
}
