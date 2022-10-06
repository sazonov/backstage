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

package com.proit.app.configuration.ddl;

import com.proit.app.domain.VersionScheme;
import com.proit.app.exception.ObjectNotFoundException;
import com.proit.app.repository.VersionSchemeRepository;
import com.proit.app.service.MigrationService;
import com.proit.app.util.MigrationUtils;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DictsDDLProvider implements BeanNameAware
{
	public static final String SEPARATOR = "/";
	public static final String SQL_EXTENSION = "sql";
	public static final String MIGRATIONS_PATH = "db/migration/dicts";

	private final VersionSchemeRepository versionSchemeRepository;

	private final MigrationService migrationService;

	@Setter
	private String beanName;

	public String getName()
	{
		return beanName;
	}

	public void update()
	{
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

		try
		{
			var migrationByName = new HashMap<String, String>();

			for (Resource resource : resolver.getResources("classpath:" + MIGRATIONS_PATH))
			{
				var resourceConnection = resource.getURL().openConnection();

				if (resourceConnection instanceof JarURLConnection jarURLConnection)
				{
					var file = jarURLConnection.getJarFile();
					var iterator = file.entries()
							.asIterator();

					StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
							.filter(it -> it.getName().startsWith(MIGRATIONS_PATH + SEPARATOR))
							.filter(it -> it.getName().endsWith(SQL_EXTENSION))
							.forEach(it -> {
								var splittedNames = it.getName().split(SEPARATOR);
								var name = splittedNames[splittedNames.length - 1];

								try (var fileStream = file.getInputStream(it))
								{
									migrationByName.put(name, new String(fileStream.readAllBytes()));
								}
								catch (IOException e)
								{
									throw new RuntimeException("Ошибка чтения миграции: %s.".formatted(name), e);
								}
							});
				}
				else
				{
					FileUtils.listFilesAndDirs(resource.getFile(), FalseFileFilter.INSTANCE, DirectoryFileFilter.INSTANCE)
							.stream()
							.flatMap(it -> FileUtils.listFiles(it, new String[]{SQL_EXTENSION}, false).stream())
							.forEach(file -> {
								try (var fileStream = new FileInputStream(file))
								{
									migrationByName.put(file.getName(), new String(fileStream.readAllBytes()));
								}
								catch (IOException e)
								{
									throw new RuntimeException("Ошибка чтения миграции: %s.".formatted(file.getName()), e);
								}
							});
				}
			}

			if (migrationByName.size() > 0)
			{
				var versions = migrationByName.keySet()
						.stream()
						.collect(Collectors.groupingBy(MigrationUtils::parseVersion,
								Collectors.mapping(Function.identity(), Collectors.toList())));

				versions.values().stream()
						.filter(it -> it.size() > 1)
						.findFirst()
						.ifPresent(it -> {
							throw new RuntimeException("Миграции %s имеют одинаковую версию.".formatted(String.join(", ")));
						});

				var appliedMigrations = versionSchemeRepository.findAll()
						.stream()
						.collect(Collectors.toMap(VersionScheme::getScript, Function.identity()));

				var count = migrationByName.entrySet()
						.stream()
						.peek(entry -> {
							var fullName = MIGRATIONS_PATH + SEPARATOR + entry.getKey();

							if (appliedMigrations.containsKey(fullName) && !isMigrationHashEquals(appliedMigrations, entry.getKey(), entry.getValue()))
							{
								//	FIXME: реализовано в рамках CPEITP-1017. Вернуть предыдущий вариант после того, как на всех контурах,
								//	 использующих дикты будет произведен деплой с версией базовой либы 4.2.55
								repairChecksum(entry.getValue(), fullName);
//								throw new RuntimeException("Не совпадает хэш-сумма миграции: %s".formatted(entry.getKey()));
							}
						})
						.filter(entry -> !appliedMigrations.containsKey(MIGRATIONS_PATH + SEPARATOR + entry.getKey()))
						.sorted(MigrationUtils.versionComparator())
						.peek(migrationService::migrate)
						.count();

				log.info("Обработано новых миграций: {}.", count);
				log.info("Успешно валидированно миграций: {}.", migrationByName.size());
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException("Ошибка обработки миграций", e);
		}
	}

	//	FIXME: реализовано в рамках CPEITP-1017
	private void repairChecksum(String script, String fullName)
	{
		var appliedMigration = versionSchemeRepository.findByScript(fullName)
				.orElseThrow(() -> new ObjectNotFoundException(VersionScheme.class, fullName));

		appliedMigration.setChecksum(MigrationUtils.getFileHash(script));

		versionSchemeRepository.save(appliedMigration);
	}

	private boolean isMigrationHashEquals(Map<String, VersionScheme> migrations, String migrationName, String script)
	{
		return migrations.get(MIGRATIONS_PATH + SEPARATOR + migrationName)
				.getChecksum()
				.equals(MigrationUtils.getFileHash(script));
	}
}
