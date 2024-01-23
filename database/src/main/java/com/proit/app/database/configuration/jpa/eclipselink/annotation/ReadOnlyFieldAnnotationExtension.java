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

package com.proit.app.database.configuration.jpa.eclipselink.annotation;

import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.sessions.SessionEvent;
import org.eclipse.persistence.sessions.SessionEventAdapter;
import org.springframework.util.ReflectionUtils;

public class ReadOnlyFieldAnnotationExtension extends SessionEventAdapter
{
	@Override
	public void preLogin(final SessionEvent event)
	{
		event.getSession()
				.getDescriptors()
				.forEach((clazz, descriptor) -> descriptor.getMappings()
						.forEach(mapping -> {
							if (hasReadOnlyColumnAnnotation(clazz, mapping))
							{
								mapping.setIsReadOnly(true);
							}
						})
				);
	}

	private boolean hasReadOnlyColumnAnnotation(Class<?> clazz, DatabaseMapping mapping)
	{
		var attributeName = mapping.getAttributeName();
		var field = ReflectionUtils.findField(clazz, attributeName);

		return field != null && field.isAnnotationPresent(ReadOnlyColumn.class);
	}
}
