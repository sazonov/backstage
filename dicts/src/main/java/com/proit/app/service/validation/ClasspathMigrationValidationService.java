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

package com.proit.app.service.validation;

import com.proit.app.exception.migration.MigrationAppliedException;
import com.proit.app.service.ddl.ast.expression.Expression;
import com.proit.app.service.ddl.ast.expression.table.CreateTable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClasspathMigrationValidationService
{
	//TODO: после реализации наличия 2-х и более engine в одной миграции, актуализировать
	public void validateMigration(String migration, List<Expression> expressions)
	{
		var engineMap = expressions.stream()
				.filter(it -> it.getClass().equals(CreateTable.class))
				.map(it -> ((CreateTable) it))
				.filter(it -> it.getEngine().getValue() != null)
				.collect(Collectors.groupingBy(it -> it.getEngine().getValue(), Collectors.toList()));

		if (engineMap.size() > 1)
		{
			throw new MigrationAppliedException(migration, "Недоступно наличие более одного engine в одной миграции.");
		}
	}
}
