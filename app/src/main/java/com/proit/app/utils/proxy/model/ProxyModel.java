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
import com.proit.app.utils.proxy.NoProxy;
import lombok.Getter;

import javax.persistence.Entity;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

@Getter
public class ProxyModel
{
	private final Class<?> clazz;

	private final List<AbstractFieldProxy> fieldProxies = new ArrayList<>();

	private Constructor<?> constructor;

	public ProxyModel(Class<?> clazz)
	{
		this.clazz = clazz;

		build();
	}

	public Object process(Object source)
	{
		Object target;

		try
		{
			target = constructor.newInstance();
		}
		catch (Exception e)
		{
			throw new RuntimeException(String.format("failed to create new instance of type '%s'", clazz.getSimpleName()), e);
		}

		fieldProxies.forEach(it -> it.process(source, target));

		return target;
	}

	private void build()
	{
		try
		{
			constructor = clazz.getConstructor();

			if (constructor.getParameterCount() > 0 || !Modifier.isPublic(constructor.getModifiers()))
			{
				throw new RuntimeException(String.format("class '%s' doesn't have public no arg constructor", clazz.getSimpleName()));
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(String.format("class '%s' doesn't have public no arg constructor", clazz.getSimpleName()), e);
		}

		List<Field> declaredFields = new ArrayList<>();
		var currentClass = clazz;

		while (currentClass != null)
		{
			declaredFields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
			currentClass = currentClass.getSuperclass();
		}

		for (Field field : declaredFields)
		{
			String fieldName = field.getName();
			Class<?> fieldType = field.getType();

			if (field.getAnnotation(NoProxy.class) != null || fieldType.getAnnotation(NoProxy.class) != null)
			{
				continue;
			}

			int fieldModifiers = field.getModifiers();

			if (Modifier.isStatic(fieldModifiers) || Modifier.isFinal(fieldModifiers) || Modifier.isTransient(fieldModifiers) || fieldName.startsWith("_"))
			{
				continue;
			}

			if (Collection.class.isAssignableFrom(fieldType))
			{
				fieldProxies.add(new CollectionFieldProxy(field));
			}
			else if (Map.class.isAssignableFrom(fieldType))
			{
				fieldProxies.add(new MapFieldProxy(field));
			}
			else if (fieldType.isArray())
			{
				throw new RuntimeException("unable to proxy arrays");
			}
			else
			{
				if (fieldType.getAnnotation(Entity.class) != null)
				{
					if (field.getAnnotation(ForceProxy.class) == null)
					{
						continue;
					}

					fieldProxies.add(new EntityFieldProxy(field));
				}
				else
				{
					fieldProxies.add(new BasicFieldProxy(field));
				}
			}

			field.setAccessible(true);
		}
	}
}
