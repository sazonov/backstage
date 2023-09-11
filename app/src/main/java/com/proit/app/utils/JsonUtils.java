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

package com.proit.app.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proit.app.exception.AppException;
import com.proit.app.model.api.ApiStatusCodeImpl;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class JsonUtils implements ApplicationContextAware
{
	private static ObjectMapper mapper;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
	{
		mapper = applicationContext.getBean(ObjectMapper.class);
	}

	public static String asJson(Object value)
	{
		try
		{
			return mapper.writeValueAsString(value);
		}
		catch (JsonProcessingException e)
		{
			throw new AppException(ApiStatusCodeImpl.SERIALIZE_ERROR,
					"При сериализации обьекта '%s', произошла ошибка: %s".formatted(value, e));
		}
	}

	public static Map<String, Object> attributeValueMap(Object value)
	{
		try
		{
			return mapper.readValue(value.toString(), new TypeReference<>() { });
		}
		catch (JsonProcessingException e)
		{
			throw new AppException(ApiStatusCodeImpl.DESERIALIZE_ERROR,
					"При десериализации обьекта '%s', произошла ошибка: %s".formatted(value, e));
		}
	}

	public static List<Map<String, Object>> attributeValueMaps(Object value)
	{
		try
		{
			return mapper.readValue(value.toString(), new TypeReference<>() { });
		}
		catch (JsonProcessingException e)
		{
			throw new AppException(ApiStatusCodeImpl.DESERIALIZE_ERROR,
					"При десериализации обьекта '%s', произошла ошибка: %s".formatted(value, e));
		}
	}

	public static Object asTypeReference(Object value, Class<?> clazz)
	{
		try
		{
			return mapper.readValue(value.toString(), mapper.constructType(clazz));
		}
		catch (JsonProcessingException e)
		{
			throw new AppException(ApiStatusCodeImpl.DESERIALIZE_ERROR,
					"При десериализации обьекта '%s', произошла ошибка: %s".formatted(value, e));
		}
	}
}
