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

package com.proit.app.cache.utils.proxy.model;

import lombok.RequiredArgsConstructor;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

@RequiredArgsConstructor
public abstract class AbstractFieldProxy
{
	protected final Field field;

	public final void process(Object source, Object target)
	{
		try
		{
			processField(source, target);
		}
		catch (Exception e)
		{
			throw new RuntimeException(String.format("failed to process field '%s.%s'", source.getClass().getSimpleName(), field.getName()), e);
		}
	}

	protected abstract void processField(Object source, Object target) throws Exception;

	protected Object getFieldValue(Object source)
	{
		try
		{
			BeanInfo beanInfo = Introspector.getBeanInfo(source.getClass());

			for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors())
			{
				if (!field.getName().equals(descriptor.getName()))
				{
					continue;
				}

				var readMethod = descriptor.getReadMethod();

				if (readMethod != null && readMethod.canAccess(source))
				{
					return readMethod.invoke(source);
				}
			}

			return field.get(source);
		}
		catch (Exception e)
		{
			throw new RuntimeException(String.format("failed to get value of the proxy field '%s.%s'", source.getClass().getSimpleName(), field.getName()), e);
		}
	}
}
