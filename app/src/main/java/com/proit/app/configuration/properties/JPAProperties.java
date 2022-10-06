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

@Setter
@Getter
@ConfigurationProperties("app.jpa")
public class JPAProperties
{
	public static final String ACTIVATION_PROPERTY = "app.jpa.enabled";
	public static final String META_MODEL_VALIDATION_ACTIVATION_PROPERTY = "app.jpa.validate-meta-model";

	private boolean enabled = true;

	/**
	 * Схема по умолчанию для работы с доменными объектами.
	 */
	private String defaultScheme;

	/**
	 * Если флаг установлен, то активируется поддержка Postgis.
	 */
	private boolean postgisEnabled;

	/**
	 * Включает проверку корректной инициализации полей в сгенерированных моделях EntityClass_.
	 */
	private boolean validateMetaModel = true;
}
