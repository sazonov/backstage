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

package com.proit.app.model;

import com.proit.app.model.api.ApiStatusCategory;
import com.proit.app.model.api.ApiStatusCode;
import lombok.Getter;

@Getter
public enum DictsStatusCodeImpl implements ApiStatusCode
{
	MIGRATION_APPLIED_ERROR(1, "Ошибка применения миграции."),
	MIGRATIONS_HAS_SAME_VERSION(2, "Миграции имеет одинаковую версию."),
	MIGRATION_FILE_READ_EXCEPTION(4, "Ошибка чтения миграции из файла."),
	MIGRATION_PROCESS_UNKNOWN_ERROR(3, "Неизвестная ошибка при обработки миграций.");

	private final Integer code;

	private final String message;

	private final ApiStatusCategory category;

	DictsStatusCodeImpl(Integer code, String message)
	{
		this(code, message, ApiStatusCategory.OTHER);
	}

	DictsStatusCodeImpl(Integer code, String message, ApiStatusCategory category)
	{
		this.code = code;
		this.message = message.isEmpty() ? this.toString() : message;
		this.category = category;
	}
}
