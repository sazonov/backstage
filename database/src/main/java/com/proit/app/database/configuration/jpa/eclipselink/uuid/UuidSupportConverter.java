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

package com.proit.app.database.configuration.jpa.eclipselink.uuid;

import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.sessions.Session;

import java.sql.Types;
import java.util.UUID;

class UuidSupportConverter implements Converter
{
	@Override
	public void initialize(DatabaseMapping mapping, Session session)
	{
		final var field = mapping.getField();

		field.setTypeName(null);
		field.setType(UUID.class);
		field.setSqlType(Types.OTHER);
	}

	@Override
	public Object convertObjectValueToDataValue(Object objectValue, Session session)
	{
		return objectValue instanceof String stringValue
				? UUID.fromString(stringValue)
				: null;
	}

	@Override
	public Object convertDataValueToObjectValue(Object dataValue, Session session)
	{
		return dataValue == null
				? null
				: dataValue.toString();
	}

	@Override
	public boolean isMutable()
	{
		return false;
	}
}
