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

package com.proit.app.utils.proxy.model;

import com.proit.app.utils.proxy.ReadOnlyObjectProxyFactory;

import java.lang.reflect.Field;

public class EntityFieldProxy extends AbstractFieldProxy
{
	EntityFieldProxy(Field field)
	{
		super(field);
	}

	protected void processField(Object source, Object target) throws Exception
	{
		var value = getFieldValue(source);

		if (value != null)
		{
			field.set(target, ReadOnlyObjectProxyFactory.createProxy(value, value.getClass()));
		}
	}
}
