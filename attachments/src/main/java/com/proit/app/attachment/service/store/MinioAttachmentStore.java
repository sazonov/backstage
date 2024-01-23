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

package com.proit.app.attachment.service.store;

import com.proit.app.attachment.configuration.properties.AttachmentProperties;
import com.proit.app.exception.AppException;
import com.proit.app.model.other.exception.ApiStatusCodeImpl;
import com.proit.app.attachment.model.domain.Attachment;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;

@Slf4j
@Component
@ConditionalOnProperty(AttachmentProperties.MinioProperties.ACTIVATION_PROPERTY)
@RequiredArgsConstructor
public class MinioAttachmentStore implements AttachmentStore
{
	private final AttachmentProperties attachmentProperties;

	private MinioClient minioClient;
	private String bucket;

	@PostConstruct
	private void initialize()
	{
		try
		{
			var config = attachmentProperties.getMinio();
			bucket = config.getBucket();

			minioClient = MinioClient.builder()
					.endpoint(config.getEndpoint())
					.credentials(config.getAccessKey(), config.getSecretKey())
					.build();

			if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))
			{
				minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
			}
		}
		catch (Exception e)
		{
			throw new AppException(ApiStatusCodeImpl.ATTACHMENT_STORE_INIT_FAILED, e);
		}
	}

	public AttachmentProperties.StoreType getType()
	{
		return AttachmentProperties.StoreType.MINIO;
	}

	public boolean attachmentExists(Attachment attachment)
	{
		try
		{
			minioClient.statObject(StatObjectArgs.builder()
					.bucket(bucket)
					.object(attachment.getId())
					.build());
		}
		catch (ErrorResponseException e)
		{
			if ("NoSuchKey".equals(e.errorResponse().code()))
			{
				return false;
			}
			else
			{
				throw new AppException(ApiStatusCodeImpl.ATTACHMENT_STORE_ERROR, e);
			}
		}
		catch (Exception e)
		{
			throw new AppException(ApiStatusCodeImpl.ATTACHMENT_STORE_ERROR, e);
		}

		return true;
	}

	public Resource getAttachment(Attachment attachment)
	{
		try
		{
			return new InputStreamResource(minioClient.getObject(GetObjectArgs.builder()
					.bucket(bucket)
					.object(attachment.getId())
					.build()));
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
			minioClient.putObject(PutObjectArgs.builder()
					.bucket(bucket)
					.contentType(attachment.getMimeType())
					.object(attachment.getId())
					.stream(stream, attachment.getSize(), -1)
					.build());
		}
		catch (Exception e)
		{
			throw new AppException(ApiStatusCodeImpl.ATTACHMENT_ADD_ERROR, e);
		}
	}

	public void deleteAttachment(Attachment attachment)
	{
		try
		{
			minioClient.removeObject(RemoveObjectArgs.builder()
					.bucket(bucket)
					.object(attachment.getId())
					.build());
		}
		catch (Exception e)
		{
			throw new AppException(ApiStatusCodeImpl.ATTACHMENT_DELETE_ERROR, e);
		}
	}
}
