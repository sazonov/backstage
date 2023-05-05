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

package com.proit.app.utils.proxy.model;

import com.proit.app.utils.proxy.ForceProxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

public class MapFieldProxy extends AbstractFieldProxy
{
	private Constructor<? extends Map> mapConstructor;

	MapFieldProxy(Field field)
	{
		super(field);
	}

	protected void processField(Object source, Object target) throws Exception
	{
		if (field.getAnnotation(ForceProxy.class) == null)
		{
			return;
		}

		var fieldValue = getFieldValue(source);

		if (fieldValue == null)
		{
			return;
		}

		field.set(target, createMapProxy((Map<?, ?>) fieldValue));
	}

	@SuppressWarnings("unchecked")
	private <K, V> Map<K, V> createMapProxy(Map<K, V> map) throws Exception
	{
		if (map.isEmpty())
		{
			return Map.of();
		}

		if (mapConstructor == null)
		{
			try
			{
				mapConstructor = map.getClass().getConstructor();
			}
			catch (Exception ignore)
			{
				// no handling
			}
		}

		if (mapConstructor == null)
		{
			return Map.copyOf(map);
		}
		else
		{
			var proxy = mapConstructor.newInstance();
			proxy.putAll(map);

			return proxy;
		}
	}
}
