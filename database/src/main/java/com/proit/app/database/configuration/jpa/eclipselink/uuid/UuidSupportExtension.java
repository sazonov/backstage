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

package com.proit.app.database.configuration.jpa.eclipselink.uuid;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.DirectToFieldMapping;
import org.eclipse.persistence.mappings.ForeignReferenceMapping;
import org.eclipse.persistence.sessions.DatabaseSession;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.sessions.SessionEvent;
import org.eclipse.persistence.sessions.SessionEventAdapter;
import org.eclipse.persistence.tools.schemaframework.SchemaManager;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * Расширение для eclipselink, которое позволяет конвертировать тип String, в зависимости от типа столбца в БД.
 * <p>
 * Если тип столбца в БД uuid - то применяем для него {@link UuidSupportConverter}
 */
@Slf4j
public class UuidSupportExtension extends SessionEventAdapter
{
	/**
	 * Type name
	 */
	private static final String UUID = "uuid";

	/**
	 * Metadata column's
	 */
	private static final String TYPE_NAME = "TYPE_NAME";
	private static final String TABLE_NAME = "TABLE_NAME";
	private static final String TABLE_SCHEM = "TABLE_SCHEM";
	private static final String COLUMN_NAME = "COLUMN_NAME";

	private static final String DEFAULT_SCHEMA = "public";

	@Override
	public void postLogin(SessionEvent event)
	{
		var databaseSession = castToDatabaseSession(event.getSession());
		var schemaManager = new SchemaManager(databaseSession);

		log.info("Obtaining meta-information.");

		var aggregatedMetadata = getAggregatedTableMetadata(schemaManager);

		if (aggregatedMetadata.isEmpty())
		{
			log.info("No UUID conversion mappings found!");

			return;
		}

		event.getSession()
				.getDescriptors()
				.forEach((clazz, descriptor) -> descriptor.getMappings()
						.stream()
						.filter(mapping -> checkIfExistInMetadata(mapping, aggregatedMetadata))
						.forEach(mapping -> updateDatabaseMapping(mapping, databaseSession)));

		super.postLogin(event);
	}

	private void updateDatabaseMapping(DatabaseMapping mapping, DatabaseSession databaseSession)
	{
		// Сет UUID типа для прямых маппингов и инициализация конвертера
		if (mapping instanceof DirectToFieldMapping directMapping)
		{
			initUuidConversion(directMapping, databaseSession);

			log.info("Type changed to uuid for mapping [ {} --> {} ], converter {} added.", directMapping.getAttributeName(), directMapping.getField(), UuidSupportConverter.class);
		}

		// Сет UUID типа для маппингов FK
		if (mapping instanceof ForeignReferenceMapping foreignMapping)
		{
			foreignMapping.getFields()
					.forEach(field -> {
						updateFieldType(field);

						log.info("Type changed to uuid for mapping  [ {} --> {} ].", foreignMapping.getAttributeName(), field);
					});
		}
	}

	private boolean checkIfExistInMetadata(DatabaseMapping databaseMapping, Map<String, Map<String, List<String>>> aggregatedMetadata)
	{
		var field = databaseMapping.getField();

		if (field != null)
		{
			return checkIfFieldExistsInMetadata(aggregatedMetadata, field);
		}
		else
		{
			return databaseMapping.getFields()
					.stream()
					.anyMatch(databaseField -> checkIfFieldExistsInMetadata(aggregatedMetadata, databaseField));
		}
	}

	private boolean checkIfFieldExistsInMetadata(Map<String, Map<String, List<String>>> aggregatedMetadata, DatabaseField field)
	{
		var entitySchema = field.getTable().getTableQualifier();

		var schema = StringUtils.isBlank(entitySchema)
				? DEFAULT_SCHEMA
				: entitySchema;

		var tableName = field.getTableName().toLowerCase();
		var fieldName = field.getName().toLowerCase();

		var tables = aggregatedMetadata.getOrDefault(schema, Map.of());
		var columns = tables.get(tableName);

		return columns != null && columns.contains(fieldName);
	}

	private void updateFieldType(DatabaseField field)
	{
		field.setTypeName(null);
		field.setType(java.util.UUID.class);
		field.setSqlType(Types.OTHER);
	}

	private void initUuidConversion(DirectToFieldMapping mapping, DatabaseSession databaseSession)
	{
		var converter = new UuidSupportConverter();
		converter.initialize(mapping, databaseSession);
		mapping.setConverter(converter);
	}

	private Map<String, Map<String, List<String>>> getAggregatedTableMetadata(SchemaManager schemaManager)
	{
		return getAggregatedTableMetadata(schemaManager, null, null, null, null, UUID);
	}

	private Map<String, Map<String, List<String>>> getAggregatedTableMetadata(SchemaManager schemaManager, String catalog,
	                                                             String schema, String table, String column, String columnType)
	{
		return ((Vector<AbstractRecord>) schemaManager.getColumnInfo(catalog, schema, table, column))
				.stream()
				.filter(metaRecord -> columnType.equalsIgnoreCase((String) metaRecord.get(TYPE_NAME)))
				.collect(Collectors.groupingBy(metaRecord -> (String) metaRecord.get(TABLE_SCHEM),
						Collectors.groupingBy(metaRecord -> (String) metaRecord.get(TABLE_NAME),
								Collectors.mapping(rec -> (String) rec.get(COLUMN_NAME), Collectors.toList()))));
	}

	private DatabaseSession castToDatabaseSession(Session session)
	{
		if (session instanceof DatabaseSession databaseSession)
		{
			return databaseSession;
		}

		throw new IllegalArgumentException("cannot cast an instance of %s to DatabaseSession".formatted(session.getClass()));
	}
}
