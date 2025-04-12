/*
 *    Copyright 2019-2024 the original author or authors.
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

package com.proit.app.dict.configuration.properties;

import com.proit.app.database.configuration.properties.DDLProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.Set;

@Getter
@Setter
@ConfigurationProperties("app.dicts")
@Validated
public class DictsProperties
{
	public static final String ENGINE_PROPERTY = "app.dicts.engines";
	public static final String STORAGE_PROPERTY = "app.dicts.storage";
	public static final String ACTIVATION_PROPERTY = "app.dicts.enabled";
	public static final String DDL_ACTIVATION_PROPERTY = "app.dicts.ddl.enabled";
	public static final String DEFAULT_ENGINE_PROPERTY = "app.dicts.default-engine";
	public static String DEFAULT_ENGINE;

	private boolean enabled = false;

	/**
	 * Параметры для DDLProvider. Схема указывается для PostgreSQL.
	 */
	private DDLProperties ddl = new DDLProperties();

	@NotBlank
	private String storage;

	@NotEmpty
	private Set<String> engines;

	@NotBlank
	private String defaultEngine;

	public void setDefaultEngine(String defaultEngine)
	{
		this.defaultEngine = defaultEngine;

		DictsProperties.DEFAULT_ENGINE = defaultEngine;
	}
}
