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

package com.proit.app.conversion.dto.audit;

import com.proit.app.configuration.properties.AuditProperties;
import com.proit.app.conversion.dto.AbstractConverter;
import com.proit.app.model.domain.audit.Audit;
import com.proit.app.model.dto.audit.AuditEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
@ConditionalOnProperty(value = AuditProperties.ACTIVATION_PROPERTY, matchIfMissing = true)
public class AuditConverter extends AbstractConverter<Audit, AuditEvent>
{
	@Override
	public AuditEvent convert(Audit source)
	{
		var target = new AuditEvent();

		target.setType(source.getType());
		target.setUserId(source.getUserId());
		target.setObjectId(source.getObjectId());
		target.setDate(source.getDate().atZone(ZoneId.systemDefault()));
		target.setSuccess(source.isSuccess());
		target.setFields(source.getProperties().getFields());
		target.setProperties(source.getProperties().getProperties());

		return target;
	}
}
