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

package com.proit.app.database.configuration.jpa.eclipselink.postgis;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.DirectToFieldMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.mappings.converters.SerializedObjectConverter;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.sessions.SessionEvent;
import org.eclipse.persistence.sessions.SessionEventAdapter;
import org.geolatte.geom.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Vector;

public class GeolatteExtension extends SessionEventAdapter
{
	@Override
	public void preLogin(final SessionEvent event)
	{
		final Session session = event.getSession();

		final Map<Class, ClassDescriptor> descriptorMap = session.getDescriptors();

		// Walk through all descriptors...
		for (final Map.Entry<Class, ClassDescriptor> entry : descriptorMap.entrySet())
		{
			final ClassDescriptor desc = entry.getValue();
			final Vector<DatabaseMapping> mappings = desc.getMappings();

			// walk through all mappings for some class...
			for (final DatabaseMapping mapping : mappings)
			{
				if (mapping instanceof DirectToFieldMapping)
				{
					final DirectToFieldMapping dfm = (DirectToFieldMapping) mapping;

					if (isCandidateConverter(dfm))
					{
						final Class type = entry.getKey();
						final String attributeName = dfm.getAttributeName();
						final Field field = getField(type, attributeName);
						final Class<?> fieldType = field.getType();

						if (Point.class == fieldType
								|| Polygon.class == fieldType
								|| LinearRing.class == fieldType
								|| LineString.class == fieldType
								|| MultiPoint.class == fieldType
								|| MultiPolygon.class == fieldType
								|| MultiLineString.class == fieldType
								|| Geometry.class == fieldType
								|| GeometryCollection.class == fieldType)
						{
							final Converter converter = new PostgisConverter();
							converter.initialize(mapping, session);
							dfm.setConverter(converter);
						}
					}
				}
			}
		}
	}

	/**
	 * Return the field associated with the specified name.
	 * Walk the parent class to locate the field as necessary.
	 */
	private Field getField(final Class type, final String attributeName)
	{
		Class t = type;

		while (t != Object.class)
		{
			try
			{
				return t.getDeclaredField(attributeName);
			}
			catch (final NoSuchFieldException nsfe)
			{
				//Ignore
			}

			t = t.getSuperclass();
		}

		throw new IllegalStateException("Unable to find field: " + attributeName);
	}

	// only consider mappings that are deemed to produce
	// byte[] database fields from object...
	private boolean isCandidateConverter(final DirectToFieldMapping mapping)
	{
		final Converter converter = mapping.getConverter();
		return null != converter && converter instanceof SerializedObjectConverter;
	}
}
