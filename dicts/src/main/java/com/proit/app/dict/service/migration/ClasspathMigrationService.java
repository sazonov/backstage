/*
 *
 *  Copyright 2019-2023 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.proit.app.dict.service.migration;

import com.proit.app.dict.domain.VersionScheme;
import com.proit.app.dict.exception.migration.MigrationAppliedException;
import com.proit.app.dict.service.ddl.SqlParser;
import com.proit.app.dict.util.MigrationUtils;
import com.proit.app.dict.service.backend.VersionSchemeBackend;
import com.proit.app.dict.service.ddl.Interpreter;
import com.proit.app.dict.service.validation.ClasspathMigrationValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static com.proit.app.dict.configuration.ddl.ClasspathMigrationDictsDDLProvider.MIGRATIONS_PATH;
import static com.proit.app.dict.configuration.ddl.ClasspathMigrationDictsDDLProvider.SEPARATOR;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClasspathMigrationService
{
	private final SqlParser sqlParser;
	private final Interpreter interpreter;

	private final VersionSchemeBackend versionSchemeBackend;

	private final DictTransactionProvider transactionProvider;

	private final ClasspathMigrationValidationService migrationValidationService;

	public void migrate(Map.Entry<String, String> migration)
	{
		var appliedMigration = MIGRATIONS_PATH + SEPARATOR + migration.getKey();

		try
		{
			transactionProvider.begin();

			var expressions = sqlParser.parse(migration.getValue());

			migrationValidationService.validateMigration(appliedMigration, expressions);

			interpreter.execute(expressions);

			versionSchemeBackend.saveVersionScheme(
					VersionScheme.builder()
							.id(String.valueOf(UUID.randomUUID().getLeastSignificantBits()))
							.checksum(MigrationUtils.getFileHash(migration.getValue()))
							.installed(LocalDateTime.now())
							.script(appliedMigration)
							.version(MigrationUtils.parseVersion(appliedMigration))
							.build());

			transactionProvider.commit();

			log.info("Применение миграции {} завершено успешно.", migration.getKey());
		}
		catch (Exception e)
		{
			transactionProvider.rollback();

			log.error("Ошибка применения миграции: {}", appliedMigration);

			throw new MigrationAppliedException(appliedMigration, e);
		}
	}
}
