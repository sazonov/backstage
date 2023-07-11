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

package com.proit.app.service;

import com.proit.app.domain.VersionScheme;
import com.proit.app.exception.migration.MigrationAppliedException;
import com.proit.app.service.backend.VersionSchemeBackend;
import com.proit.app.service.ddl.Interpreter;
import com.proit.app.service.ddl.SqlParser;
import com.proit.app.util.MigrationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static com.proit.app.configuration.ddl.DictsDDLProvider.MIGRATIONS_PATH;
import static com.proit.app.configuration.ddl.DictsDDLProvider.SEPARATOR;

@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationService
{
	private final SqlParser sqlParser;
	private final Interpreter interpreter;

	private final VersionSchemeBackend versionSchemeBackend;

	public void migrate(Map.Entry<String, String> migration)
	{
		var appliedMigration = MIGRATIONS_PATH + SEPARATOR + migration.getKey();

		try
		{
			versionSchemeBackend.beginDDL();

			interpreter.execute(sqlParser.parse(migration.getValue()));

			versionSchemeBackend.create(
					VersionScheme.builder()
							.id(String.valueOf(UUID.randomUUID().getLeastSignificantBits()))
							.checksum(MigrationUtils.getFileHash(migration.getValue()))
							.installed(LocalDateTime.now())
							.script(appliedMigration)
							.version(MigrationUtils.parseVersion(appliedMigration))
							.build());

			versionSchemeBackend.commitDDL();

			log.info("Применение миграции {} завершено успешно.", migration.getKey());
		}
		catch (Exception e)
		{
			versionSchemeBackend.rollbackDDL();

			log.error("Ошибка применения миграции: {}", appliedMigration);

			throw new MigrationAppliedException(appliedMigration, e);
		}
	}
}
