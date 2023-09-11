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

package com.proit.app.exception.migration.transaction;

import com.proit.app.exception.migration.MigrationException;
import com.proit.app.model.DictsStatusCodeImpl;

public class DictRollbackException extends MigrationException
{
	public DictRollbackException(String dictId, Throwable cause)
	{
		super(DictsStatusCodeImpl.MIGRATION_APPLIED_ERROR, "При откате миграции для справочника '%s' произошла ошибка.".formatted(dictId), cause);
	}
}