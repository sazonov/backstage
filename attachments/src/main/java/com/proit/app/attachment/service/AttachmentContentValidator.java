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

package com.proit.app.attachment.service;

import com.proit.app.attachment.configuration.properties.AttachmentProperties;
import com.proit.app.exception.AppException;
import com.proit.app.model.other.exception.ApiStatusCodeImpl;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.*;

@Component
@ConditionalOnProperty(name = "app.attachments.verify-content")
public class AttachmentContentValidator implements AttachmentServiceAdvice
{
	private static final Set<String> IMAGES = Set.of(
			AttachmentProperties.IMAGE_BPM_VALUE,
			AttachmentProperties.IMAGE_MS_BPM_VALUE,
			MediaType.IMAGE_JPEG_VALUE,
			MediaType.IMAGE_PNG_VALUE);

	private static final Map<String, List<Pair<Integer, byte[]>>> MAGIC_NUMBERS = new HashMap<>();

	static
	{
		List<Pair<Integer, byte[]>> zipSignatures = List.of(
				ImmutablePair.of(0, new byte[] {0x50, 0x4b, 0x03, 0x04}),
				ImmutablePair.of(0, new byte[] {0x50, 0x4b, 0x05, 0x06}),
				ImmutablePair.of(0, new byte[] {0x50, 0x4b, 0x07, 0x08}));

		MAGIC_NUMBERS.put(AttachmentProperties.APPLICATION_ZIP, zipSignatures);
		MAGIC_NUMBERS.put(AttachmentProperties.APPLICATION_DOCX, zipSignatures);
		MAGIC_NUMBERS.put(AttachmentProperties.APPLICATION_DOC, List.of(ImmutablePair.of(0, new byte[] { (byte) 0xd0, (byte) 0xcf, 0x11, (byte) 0xe0, (byte) 0xa1, (byte) 0xb1, 0x1a, (byte) 0xe1})));
		MAGIC_NUMBERS.put(MediaType.APPLICATION_PDF_VALUE, List.of(ImmutablePair.of(0, new byte[] {0x25, 0x50, 0x44, 0x46, 0x2d})));
	}

	@Override
	public void handleAddAttachment(String id, String fileName, String mimeType, String userId, byte[] data)
	{
		if (IMAGES.contains(mimeType))
		{
			try
			{
				if (ImageIO.read(new ByteArrayInputStream(data)) == null)
				{
					throw new RuntimeException("failed to read image content");
				}
			}
			catch (Exception e)
			{
				throw new AppException(ApiStatusCodeImpl.ATTACHMENT_INVALID_CONTENT);
			}
		}
		else
		{
			var signatures = MAGIC_NUMBERS.getOrDefault(mimeType, Collections.emptyList());

			if (!signatures.isEmpty())
			{
				boolean hasMatch = false;

				for (var signature : signatures)
				{
					if (hasMatch)
					{
						break;
					}

					int offset = signature.getKey();
					var bytes = signature.getValue();

					for (var i = offset; i < offset + bytes.length; i++)
					{
						if (data[i] != bytes[i - offset])
						{
							hasMatch = false;

							break;
						}

						hasMatch = true;
					}
				}

				if (!hasMatch)
				{
					throw new AppException(ApiStatusCodeImpl.ATTACHMENT_INVALID_CONTENT);
				}
			}
		}
	}
}
