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

/**
 * Класс, обновляющий структуру данных приложения. Для управления порядком применения изменений необходимо использовать
 * аннотацию {@link org.springframework.core.annotation.Order} или {@link javax.annotation.Priority}.
 * Все системные компоненты должны приоритет ниже, чем {@link DDLConfiguration#DDL_PRECEDENCE_SYSTEM}.
 * Все компоненты приложения должны иметь приоритет ниже, чем {@link DDLConfiguration#DDL_PRECEDENCE_APP}.
 */
public interface DDLProvider
{
	String getName();

	void update(String scheme);
}
