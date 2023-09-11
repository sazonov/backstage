/*
 *
 *  Copyright 2019-2023 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.proit.app.model.json;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.proit.app.exception.AppException;
import com.proit.app.model.api.ApiStatusCodeImpl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

/**
 * Компонент десериализует параметр, см. описание сериализуемого параметра {@link AbstractCustomJsonSerializer}.
 * Наследнику необходимо реализовать {@link AbstractCustomJsonDeserializer#extractedValue(JsonNode)} для осуществления логики десериализации параметра.
 *
 * @param <T> - десериализуемый обьект
 */
public abstract class AbstractCustomJsonDeserializer<T> extends JsonDeserializer<T>
{
	private static final int TYPE_TOKEN = 0;
	private static final int VALUE_TOKEN = 1;

	private JsonNode mainNode;
	private ObjectCodec mainCodec;

	@Override
	public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException
	{
		var json = (JsonNode) p.readValueAsTree();

		this.mainCodec = p.getCodec();
		this.mainNode = json;

		return extractedValue(json);
	}

	public abstract T extractedValue(JsonNode json);

	/**
	 * Извлекает значение из главной json ноды по имени поля.
	 *
	 * @param fieldName - имя поля.
	 * @return значение, не null-safety.
	 */
	protected Object extractValueByFieldName(String fieldName)
	{
		var node = mainNode.get(fieldName);

		if (node.isNull())
		{
			return null;
		}

		if (node.get(TYPE_TOKEN).isArray())
		{
			return readMultiValue(node);
		}

		return readSingleValue(node);
	}

	protected Object readMultiValue(JsonNode value)
	{
		var spliterator = Spliterators.spliteratorUnknownSize(value.get(TYPE_TOKEN).elements(), Spliterator.ORDERED);

		return StreamSupport.stream(spliterator, false)
				.filter(JsonNode::isArray)
				.map(this::readSingleValue)
				.toList();
	}

	protected Object readSingleValue(JsonNode value)
	{
		var clazz = extractClass(value);

		try (JsonParser parser = value.get(VALUE_TOKEN).traverse(mainCodec))
		{
			return parser.readValueAs(new TypeReference<>() {
				public Type getType()
				{
					return clazz;
				}
			});
		}
		catch (IOException e)
		{
			throw new AppException(ApiStatusCodeImpl.DESERIALIZE_ERROR, e);
		}
	}

	private Class<?> extractClass(JsonNode value)
	{
		try
		{
			return Class.forName(value.get(TYPE_TOKEN).asText());
		}
		catch (ClassNotFoundException e)
		{
			throw new AppException(ApiStatusCodeImpl.DESERIALIZE_ERROR, e);
		}
	}
}
