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

package com.proit.app.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.internal.database.postgresql.PostgreSQLConfigurationExtension;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;

@Slf4j
@UtilityClass
public class DDLUtils
{
	public static void applyDDL(String application, String scheme, DataSource dataSource)
	{
		applyDDL(application, scheme, dataSource, Collections.emptyMap());
	}

	public static void applyDDL(String application, String scheme, DataSource dataSource, Map<String, String> placeholders)
	{
		if (StringUtils.isBlank(application))
		{
			throw new IllegalArgumentException("application name is not specified.");
		}

		// Находим миграционные файлы и применяем их.
		var builder = Flyway.configure()
				.table("schema_version_" + application)
				.encoding("UTF-8")
				.baselineOnMigrate(true)
				.dataSource(dataSource)
				.locations("/db/migration/" + application)
				.placeholders(placeholders);

		if (StringUtils.isNotBlank(scheme))
		{
			builder.schemas(scheme);
		}

		var flyway = builder.load();

		var configurationExtension = flyway.getConfiguration().getPluginRegister().getPlugin(PostgreSQLConfigurationExtension.class);
		configurationExtension.setTransactionalLock(false);

		try
		{
			flyway.migrate();
		}
		catch (FlywayException fwe)
		{
			log.error("Произошла ошибка миграции. Попробуем еще раз после вызова repair.", fwe);

			try
			{
				flyway.repair();
				flyway.migrate();
			}
			catch (FlywayException fwe2)
			{
				throw new RuntimeException("failed to apply database migration", fwe2);
			}
		}
	}
}
