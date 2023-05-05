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

package com.proit.app.conversion.dto.attachment;

import com.proit.app.configuration.properties.AttachmentProperties;
import com.proit.app.conversion.dto.AbstractConverter;
import com.proit.app.model.domain.AttachmentBinding;
import com.proit.app.model.dto.attachment.AttachmentBindingDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
@ConditionalOnProperty(AttachmentProperties.ACTIVATION_PROPERTY)
public class AttachmentBindingConverter extends AbstractConverter<AttachmentBinding, AttachmentBindingDto>
{
	@Override
	public AttachmentBindingDto convert(AttachmentBinding source)
	{
		var target = new AttachmentBindingDto();

		target.setAttachmentId(source.getAttachmentId());
		target.setCreated(source.getCreated().atZone(ZoneId.systemDefault()));
		target.setObjectId(source.getObjectId());
		target.setType(source.getType());
		target.setUserId(source.getUserId());

		return target;
	}
}
