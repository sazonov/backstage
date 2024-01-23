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

package com.proit.app.cache.utils.proxy;

import com.proit.app.cache.exception.ReadOnlyModificationException;
import com.proit.app.cache.utils.proxy.model.ProxyModel;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ReadOnlyObjectProxyFactory
{
	private static final Map<String, ProxyModel> PROXY_MODELS = new HashMap<>();

	private static final String SETTER_METHOD_PREFIX = "set";
	private static final String METHOD_CAN_EQUAL = "canEqual";
	private static final String METHOD_EQUALS = "equals";
	private static final String METHOD_WRITE_REPLACE = "writeReplace";
	private static final String METHOD_GET_ORIGINAL_OBJECT = "unwrapOriginalObject";

	private static class ReadOnlyMethodInterceptor implements MethodInterceptor, Serializable
	{
		private final Object object;

		ReadOnlyMethodInterceptor(Object object)
		{
			this.object = object;
		}

		public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable
		{
			if (method.getName().startsWith(SETTER_METHOD_PREFIX))
			{
				throw new ReadOnlyModificationException();
			}
			else if (method.getName().equals(METHOD_EQUALS) || method.getName().equals(METHOD_CAN_EQUAL))
			{
				if (method.getName().equals(METHOD_CAN_EQUAL))
				{
					method.setAccessible(true);
				}

				if (objects.length > 0 && objects[0] instanceof ReadOnlyProxy otherObject)
				{
					return method.invoke(object, otherObject.unwrapOriginalObject());
				}
			}
			else if (method.getName().equals(METHOD_GET_ORIGINAL_OBJECT))
			{
				return object;
			}
			else if (method.getName().equals(METHOD_WRITE_REPLACE))
			{
				return object;
			}

			return method.invoke(object, objects);
		}
	}

	public static <T> T createProxy(Object source, Class<T> targetClass)
	{
		String canonicalName = targetClass.getCanonicalName();

		if (!PROXY_MODELS.containsKey(canonicalName))
		{
			// FIXME: сделать нормальную блокировку
			synchronized (ReadOnlyObjectProxyFactory.class)
			{
				PROXY_MODELS.put(canonicalName, new ProxyModel(targetClass));
			}
		}

		return targetClass.cast(Enhancer.create(
				targetClass,
				new Class[] { ReadOnlyProxy.class, Serializable.class },
				new ReadOnlyMethodInterceptor(PROXY_MODELS.get(canonicalName).process(source))));
	}
}