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

package com.proit.app.attachment.conversion.dto;

import com.proit.app.attachment.configuration.properties.AttachmentProperties;
import com.proit.app.attachment.model.domain.Attachment;
import com.proit.app.attachment.model.dto.AttachmentDto;
import com.proit.app.attachment.repository.AttachmentBindingRepository;
import com.proit.app.conversion.dto.AbstractConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(AttachmentProperties.ACTIVATION_PROPERTY)
@RequiredArgsConstructor
public class AttachmentConverter extends AbstractConverter<Attachment, AttachmentDto>
{
	private static final String DEFAULT_ATTACHMENT_URL = "/api/attachment/get/%s";

	private final AttachmentBindingRepository attachmentBindingRepository;
	private final AttachmentBindingConverter attachmentBindingConverter;
	private final AttachmentProperties attachmentProperties;

	@Override
	public AttachmentDto convert(Attachment source)
	{
		return convert(Collections.singletonList(source)).get(0);
	}

	public List<AttachmentDto> convert(List<Attachment> sources)
	{
		List<AttachmentDto> targets = new ArrayList<>(sources.size());

		if (sources.isEmpty())
		{
			return Collections.emptyList();
		}

		var bindings = attachmentBindingRepository.findByAttachmentIds(sources.stream().map(Attachment::getId).collect(Collectors.toList()));

		sources.forEach(source -> {
			var target = new AttachmentDto();

			target.setId(source.getId());
			target.setCreated(source.getCreated().atZone(ZoneId.systemDefault()));
			target.setFileName(source.getFileName());
			target.setMimeType(source.getMimeType());
			target.setUserId(source.getUserId());
			target.setSize(source.getSize());
			target.setBindings(attachmentBindingConverter.convert(bindings.getOrDefault(source.getId(), List.of())));

			targets.add(target);
		});

		if (attachmentProperties.getBaseUrl() != null)
		{
			targets.forEach(target -> {
				target.setUrl(UriComponentsBuilder.fromHttpUrl(attachmentProperties.getBaseUrl()).path(String.format(DEFAULT_ATTACHMENT_URL, target.getId())).toUriString());
			});
		}

		return targets;
	}
}
