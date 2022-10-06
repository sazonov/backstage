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

package com.proit.app.conversion.jpa;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.sessions.Session;
import org.postgresql.util.PGobject;

public abstract class AbstractJsonConverter<T> implements Converter
{
	protected ObjectMapper mapper;

	public void initialize(DatabaseMapping mapping, Session session)
	{
		createObjectMapper();
	}

	public Object convertObjectValueToDataValue(Object objectValue, Session session)
	{
		try
		{
			PGobject pgObject = new PGobject();
			pgObject.setType("jsonb");
			pgObject.setValue(mapper.writeValueAsString(objectValue));

			return pgObject;
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("can't convert to jsonb", e);
		}
	}

	public T convertDataValueToObjectValue(Object dataValue, Session session)
	{
		try
		{
			if (dataValue instanceof PGobject && ((PGobject) dataValue).getType().equals("jsonb"))
			{
				return mapper.readerFor(createTypeReference()).readValue(((PGobject) dataValue).getValue());
			}

			return createDefaultValue();
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("can't convert from jsonb", e);
		}
	}

	public boolean isMutable()
	{
		return false;
	}

	protected abstract TypeReference<T> createTypeReference();

	protected T createDefaultValue()
	{
		return null;
	}

	protected void createObjectMapper()
	{
		mapper = new ObjectMapper()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.registerModule(new JavaTimeModule());
	}
}
