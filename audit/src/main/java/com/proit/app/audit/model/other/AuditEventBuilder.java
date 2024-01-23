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

package com.proit.app.audit.model.other;

import com.proit.app.audit.model.domain.AuditPropertiesField;
import com.proit.app.audit.model.domain.AuditPropertiesProperty;
import com.proit.app.audit.model.dto.AuditEvent;

import java.time.ZonedDateTime;

public class AuditEventBuilder
{
	private final AuditEvent event;

	public static AuditEventBuilder create(Enum<?> type, String objectId)
	{
		return create(type.toString(), objectId, null);
	}

	public static AuditEventBuilder create(String type, String objectId)
	{
		return create(type, objectId, null);
	}

	public static AuditEventBuilder create(Enum<?> type, String objectId, String userId)
	{
		return new AuditEventBuilder(type.toString(), objectId, userId);
	}

	public static AuditEventBuilder create(String type, String objectId, String userId)
	{
		return new AuditEventBuilder(type, objectId, userId);
	}

	private AuditEventBuilder(String type, String objectId, String userId)
	{
		if (type == null)
		{
			throw new IllegalArgumentException("audit event type must be specified");
		}

		event = new AuditEvent();

		event.setType(type);
		event.setObjectId(objectId);
		event.setUserId(userId);
		event.setDate(ZonedDateTime.now());
		event.setSuccess(true);
	}

	public AuditEventBuilder withSuccess(boolean success)
	{
		event.setSuccess(success);

		return this;
	}

	public AuditEventBuilder withField(String name, String oldValue, String newValue)
	{
		if (newValue == null && oldValue != null)
		{
			event.getFields().add(new AuditPropertiesField(name, oldValue, null));
		}
		else if (newValue != null && oldValue == null)
		{
			event.getFields().add(new AuditPropertiesField(name, null, newValue));
		}
		else if (newValue != null && !newValue.equals(oldValue))
		{
			event.getFields().add(new AuditPropertiesField(name, oldValue, newValue));
		}

		return this;
	}

	public AuditEventBuilder withField(String name, Double oldValue, Double newValue)
	{
		var oldValueString = oldValue != null ? oldValue.toString() : null;
		var newValueString = newValue != null ? newValue.toString() : null;

		return withField(name, oldValueString, newValueString);
	}

	public AuditEventBuilder withField(String name, boolean oldValue, boolean newValue)
	{
		return withField(name, Boolean.toString(oldValue), Boolean.toString(newValue));
	}

	public AuditEventBuilder withProperty(String key, String value)
	{
		event.getProperties().add(new AuditPropertiesProperty(key, value));

		return this;
	}

	public AuditEvent build()
	{
		return event;
	}
}
