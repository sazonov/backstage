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

package com.proit.app.model.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.proit.app.exception.AppException;
import com.proit.app.model.other.exception.ApiStatusCodeImpl;

import java.io.IOException;
import java.util.Collection;

/**
 * Компонент сериализует параметр с указанием наименования типа каждого элемента структуры.
 * Например: {"id":["java.lang.String", "asd"], "collection": ["java.util.ArrayList", ["java.lang.Integer", 1], ["java.lang.Integer", 2]]}.
 * Наследнику необходимо реализовать {@link AbstractCustomJsonSerializer#serializeValue(Object, JsonGenerator)} для осуществления логики сериализации параметра.
 * См. десериализацию {@link AbstractCustomJsonDeserializer}
 *
 * @param <T> - сериализуемый обьект.
 */
public abstract class AbstractCustomJsonSerializer<T> extends JsonSerializer<T>
{
	@Override
	public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException
	{
		gen.writeStartObject();

		serializeValue(value, gen);

		gen.writeEndObject();
	}

	public abstract void serializeValue(T value, JsonGenerator gen);

	/**
	 * Сереализует значение в json ноду формата: "field": ["java.lang.ValueType", "value"].
	 *
	 * @param fieldName - наименование поля.
	 * @param value     - значение.
	 */
	protected void writeNodeWithTypePrefix(JsonGenerator gen, String fieldName, Object value)
	{
		if (value instanceof Collection<?> collection)
		{
			writeMultiValue(gen, fieldName, collection);

			return;
		}

		writeSingleValue(gen, fieldName, value);
	}

	protected void writeMultiValue(JsonGenerator gen, String fieldName, Collection<?> collection)
	{
		try
		{
			gen.writeArrayFieldStart(fieldName);
			gen.writeTypePrefix(typePrefix(collection));

			collection.forEach(it -> writeValueWithTypePrefix(gen, it));

			gen.writeEndArray();
			gen.writeEndArray();
		}
		catch (IOException e)
		{
			throw new AppException(ApiStatusCodeImpl.SERIALIZE_ERROR, e);
		}
	}

	protected void writeSingleValue(JsonGenerator gen, String fieldName, Object value)
	{
		try
		{
			gen.writeFieldName(fieldName);

			writeValueWithTypePrefix(gen, value);
		}
		catch (IOException e)
		{
			throw new AppException(ApiStatusCodeImpl.SERIALIZE_ERROR, e);
		}
	}

	private void writeValueWithTypePrefix(JsonGenerator gen, Object value)
	{
		try
		{
			if (value == null)
			{
				gen.writeNull();

				return;
			}

			gen.writeTypePrefix(typePrefix(value));
			gen.writeObject(value);
			gen.writeEndArray();
		}
		catch (IOException e)
		{
			throw new AppException(ApiStatusCodeImpl.SERIALIZE_ERROR, e);
		}
	}

	private WritableTypeId typePrefix(Object value)
	{
		var typePrefix = new WritableTypeId(value, JsonToken.NOT_AVAILABLE);
		typePrefix.include = WritableTypeId.Inclusion.WRAPPER_ARRAY;
		typePrefix.id = value.getClass().getName();

		return typePrefix;
	}
}
