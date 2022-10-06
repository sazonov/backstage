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

package com.proit.app.configuration.jpa.eclipselink.postgis;

import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.DirectCollectionMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.sessions.Session;
import org.geolatte.geom.*;
import org.geolatte.geom.codec.Wkb;
import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.codec.Wkt.Dialect;
import org.postgis.PGgeometry;

import java.lang.reflect.Field;
import java.sql.SQLException;

public class PostgisConverter implements Converter
{
	@Override
	@SuppressWarnings("unchecked")
	public Object convertObjectValueToDataValue(final Object objectValue, final Session session)
	{
		if (null == objectValue)
		{
			return null;
		}

		final Geometry geometry = (Geometry) objectValue;
		final String wkt = Wkt.newEncoder(Dialect.POSTGIS_EWKT_1).encode(geometry);

		try
		{
			return new PGgeometry(wkt);
		}
		catch (final SQLException se)
		{
			throw new IllegalStateException("failed converting geometry", se);
		}
	}

	@Override
	public Geometry convertDataValueToObjectValue(final Object dataValue, final Session session)
	{
		if (null == dataValue)
		{
			return null;
		}
		if (dataValue instanceof PGgeometry)
		{
			final org.postgis.Geometry geometry = ((PGgeometry) dataValue).getGeometry();
			return Wkt.newDecoder(Wkt.Dialect.POSTGIS_EWKT_1).decode(geometry.toString());
		}
		else if (dataValue instanceof String)
		{
			/*
			In some circumstances the data will come back in WKB format (i.e. When using the Driver directly)
			and sometimes it will be returned in WKT format (i.e. In GlassFish when using the DataSource) and
			it is unclear what is causing the variance so support both scenarios.
			*/
			final String wk = (String) dataValue;
			final char ch = wk.charAt(0);

			if ('0' == ch)
			{
				//Guess that it is in WKB format
				return Wkb.newDecoder(Wkb.Dialect.POSTGIS_EWKB_1).decode(ByteBuffer.from(wk));
			}
			else
			{
				//Assume a WKT format
				return Wkt.newDecoder(Wkt.Dialect.POSTGIS_EWKT_1).decode(wk);
			}
		}
		else
		{
			throw new IllegalStateException("unable to convert data value: " + dataValue);
		}
	}

	@Override
	public boolean isMutable()
	{
		return false;
	}

	@Override
	public void initialize(final DatabaseMapping mapping, final Session session)
	{
		final DatabaseField field;

		if (mapping instanceof DirectCollectionMapping)
		{
			field = ((DirectCollectionMapping) mapping).getDirectField();
		}
		else
		{
			field = mapping.getField();
		}

		field.setSqlType(java.sql.Types.OTHER);

		if (null == field.getColumnDefinition())
		{
			final Field javaField = getJavaField(mapping, field);
			final Class<?> javaFieldType = javaField.getType();

			//TODO: Dervive the SRS from an annotation
			//TODO: Dervive the M from an annotation
			if (Point.class == javaFieldType)
			{
				field.setColumnDefinition("geometry(POINT,-1)");
			}
			else if (Polygon.class == javaFieldType)
			{
				field.setColumnDefinition("geometry(POLYGON,-1)");
			}
			else if (LineString.class == javaFieldType)
			{
				field.setColumnDefinition("geometry(LINESTRING,-1)");
			}
			else if (MultiPoint.class == javaFieldType)
			{
				field.setColumnDefinition("geometry(MULTIPOINT,-1)");
			}
			else if (MultiPolygon.class == javaFieldType)
			{
				field.setColumnDefinition("geometry(MULTIPOLYGON,-1)");
			}
			else if (MultiLineString.class == javaFieldType)
			{
				field.setColumnDefinition("geometry(MULTILINESTRING,-1)");
			}
			else if (LinearRing.class == javaFieldType)
			{
				field.setColumnDefinition("geometry(LINEARRING,-1)");
			}
			else
			{
				field.setColumnDefinition("geometry");
			}
		}
	}

	private Field getJavaField(final DatabaseMapping mapping, final DatabaseField field)
	{
		try
		{
			final String fieldName = field.getName();
			final Class type = mapping.getDescriptor().getJavaClass();

			return type.getField(fieldName);
		}
		catch (final NoSuchFieldException nsfe)
		{
			throw new IllegalStateException("unable to locate expected field", nsfe);
		}
	}
}
