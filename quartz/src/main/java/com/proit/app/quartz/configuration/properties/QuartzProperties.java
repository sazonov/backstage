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

package com.proit.app.quartz.configuration.properties;

import com.proit.app.database.configuration.properties.DDLProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("app.quartz")
public class QuartzProperties
{
	public static final String ACTIVATION_PROPERTY = "app.quartz.enabled";

	private boolean enabled = false;

	/**
	 * Если флаг установлен, то приложение сможет создавать задания, но выполняться в нём они не будут.
	 * Может быть полезно, когда приложение распределено и в каких-то из подсистем не требуется
	 * обрабатывать запланированные задания.
	 */
	private boolean onlyForScheduling = false;

	/**
	 * Кол-во нитей, которые будут использованы для выполнения заданий.
	 */
	private Integer threadCount = 10;

	/**
	 * Идентификатор планировщика Quartz, к которому будут привязаны все задания.
	 */
	private String schedulerName = "BaseScheduler";

	/**
	 * Параметры для DDLProvider.
	 */
	private DDLProperties ddl = new DDLProperties();

	/**
	 * Поддержка кластеризации. Если не активно, используются оптимизации, существенно ускоряющие создание новых задач.
	 */
	private boolean clustered = false;
}
