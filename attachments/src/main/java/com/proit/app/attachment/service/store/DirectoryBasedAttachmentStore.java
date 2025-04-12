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

package com.proit.app.attachment.service.store;

import com.proit.app.attachment.configuration.properties.AttachmentProperties;
import com.proit.app.attachment.model.domain.Attachment;
import com.proit.app.exception.AppException;
import com.proit.app.model.other.exception.ApiStatusCodeImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
@ConditionalOnProperty(AttachmentProperties.DirectoryProperties.ACTIVATION_PROPERTY)
@RequiredArgsConstructor
public class DirectoryBasedAttachmentStore implements AttachmentStore
{
	private final AttachmentProperties attachmentProperties;

	private File storePath;

	@PostConstruct
	private void initialize()
	{
		try
		{
			storePath = new File(attachmentProperties.getDirectory().getPath());
			storePath.mkdirs();

			log.info("Setting attachment store path: {}.", storePath.getPath());

			if (!storePath.canWrite() || !storePath.canRead())
			{
				throw new RuntimeException("invalid attachment store path permissions");
			}
		}
		catch (Exception e)
		{
			throw new AppException(ApiStatusCodeImpl.ATTACHMENT_STORE_INIT_FAILED, e);
		}
	}

	public AttachmentProperties.StoreType getType()
	{
		return AttachmentProperties.StoreType.DIRECTORY;
	}

	public boolean attachmentExists(Attachment attachment)
	{
		return new File(storePath, assignPath(attachment)).exists();
	}

	public Resource getAttachment(Attachment attachment)
	{
		try
		{
			var resource = new UrlResource(new File(storePath, assignPath(attachment)).toURI());

			if (resource.exists() || resource.isReadable())
			{
				return resource;
			}
			else
			{
				throw new FileNotFoundException(resource.getURI().toString());
			}
		}
		catch (Exception e)
		{
			throw new AppException(ApiStatusCodeImpl.ATTACHMENT_DATA_NOT_AVAILABLE, e);
		}
	}

	public void saveAttachment(Attachment attachment, InputStream stream)
	{
		try
		{
			FileUtils.copyInputStreamToFile(stream, new File(storePath, assignPath(attachment)));
		}
		catch (IOException e)
		{
			throw new AppException(ApiStatusCodeImpl.ATTACHMENT_ADD_ERROR, e);
		}
	}

	public void deleteAttachment(Attachment attachment)
	{
		var targetFile = new File(storePath, assignPath(attachment));

		try
		{
			if (targetFile.exists())
			{
				if (!targetFile.delete())
				{
					throw new RuntimeException(String.format("cannot delete file '%s'", targetFile.getAbsolutePath()));
				}
			}
		}
		catch (Exception e)
		{
			throw new AppException(ApiStatusCodeImpl.ATTACHMENT_DELETE_ERROR, e);
		}
	}

	protected String assignPath(Attachment attachment)
	{
		var created = attachment.getCreated();

		return created.getYear() + File.separator + String.format("%02d", created.getMonth().getValue()) + File.separator + attachment.getId();
	}
}
