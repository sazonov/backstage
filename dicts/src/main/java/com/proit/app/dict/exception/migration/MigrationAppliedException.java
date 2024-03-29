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

package com.proit.app.dict.exception.migration;

import com.proit.app.dict.model.DictsStatusCodeImpl;

public class MigrationAppliedException extends MigrationException
{
	public MigrationAppliedException(String migration, String message)
	{
		super(DictsStatusCodeImpl.MIGRATION_APPLIED_ERROR, "Ошибка '%s' при применении миграции '%s'.".formatted(message, migration));
	}

	public MigrationAppliedException(String migration, Throwable cause)
	{
		super(DictsStatusCodeImpl.MIGRATION_APPLIED_ERROR, "Ошибка при применении миграции '%s'.".formatted(migration), cause);
	}
}
