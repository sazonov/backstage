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
import com.proit.app.utils.proxy.ReadOnlyObjectProxyFactory;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class CollectionFieldProxy extends AbstractFieldProxy
{
	CollectionFieldProxy(Field field)
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

		field.set(target, createCollectionProxy((Collection<?>) fieldValue));
	}

	@SuppressWarnings("unchecked")
	private <T> Collection<T> createCollectionProxy(Collection<T> collection)
	{
		Collection<T> collectionProxy;

		if (!collection.isEmpty())
		{
			List<T> tempCollection = new ArrayList<>();

			var element = collection.iterator().next();

			if (element instanceof String || element instanceof Enum || ClassUtils.isPrimitiveOrWrapper(element.getClass()))
			{
				tempCollection.addAll(collection);
			}
			else
			{
				collection.forEach(it -> tempCollection.add((T) ReadOnlyObjectProxyFactory.createProxy(it, it.getClass())));
			}

			if (collection instanceof List)
			{
				collectionProxy = List.copyOf(tempCollection);
			}
			else if (collection instanceof Set)
			{
				collectionProxy = Set.copyOf(tempCollection);
			}
			else
			{
				throw new RuntimeException(String.format("unknown collection type '%s'", collection.getClass().getSimpleName()));
			}
		}
		else
		{
			if (collection instanceof List)
			{
				collectionProxy = List.of();
			}
			else if (collection instanceof Set)
			{
				collectionProxy = Set.of();
			}
			else
			{
				throw new RuntimeException(String.format("unknown collection type '%s'", collection.getClass().getSimpleName()));
			}
		}

		return collectionProxy;
	}
}
