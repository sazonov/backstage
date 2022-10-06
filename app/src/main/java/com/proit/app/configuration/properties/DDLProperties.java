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

package com.proit.app.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("app.ddl")
public class DDLProperties
{
	public static final String ACTIVATION_PROPERTY = "app.ddl.enabled";

	/**
	 * Активирует инициализацию всех зарегистрированных провайдеров {@link com.proit.app.configuration.ddl.DDLProvider}
	 * при старте приложения.
	 */
	private boolean enabled = true;

	/**
	 * При активации пропускает провайдеров DDL системных компонентов, но активирует все провайдеры приложения.
	 */
	private boolean skipSystemDDL = false;

	/**
	 * Схема БД, для которой должен применяться DDL.
	 */
	private String scheme;
}
