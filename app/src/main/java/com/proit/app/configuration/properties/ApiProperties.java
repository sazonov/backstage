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

import java.util.List;

@Getter
@Setter
@ConfigurationProperties("app.api")
public class ApiProperties
{
	public static final String SWAGGER_ACTIVATION_PROPERTY = "app.api.swagger.enabled";

	@Getter
	@Setter
	public static class SwaggerProperties
	{
		/**
		 * Определяет доступность Swagger.
		 */
		private boolean enabled = true;

		/**
		 * Активирует поддержку CSRF.
		 */
		private boolean csrf = false;

		/**
		 * Ограничивает список пакетов, для которых будет доступен Swagger.
		 */
		private List<String> packagesToScan;

		/**
		 * Ограничивает пути, которые будут включены в Swagger.
		 */
		private List<String> pathsToMatch;
	}

	/**
	 * Настройки Swagger для API.
	 */
	private SwaggerProperties swagger;

	/**
	 * Активирует вывод stack trace в ответах api для необработанных исключений.
	 */
	private boolean stackTraceOnError = true;
}
