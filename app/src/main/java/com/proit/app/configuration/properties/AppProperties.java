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

package com.proit.app.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties("app")
public class AppProperties
{
	public static final String DEFAULT_PACKAGE = "com.proit";

	/**
	 * Внутренний адрес приложения.
	 */
	private String url;

	/**
	 * Внешний адрес приложения.
	 */
	private String externalUrl;

	/**
	 * Пакеты с компонентами приложения.
	 */
	private List<String> basePackages = new ArrayList<>();

	/**
	 * Идентификатор модуля приложения.
	 */
	private String module = "app";

	/**
	 * Версия приложения.
	 */
	private String version = "dev";

	public List<String> getBasePackages()
	{
		var res = new ArrayList<>(basePackages);
		res.add(DEFAULT_PACKAGE);

		return res;
	}
}
