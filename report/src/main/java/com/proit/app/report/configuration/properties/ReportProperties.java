/*
 *    Copyright 2019-2023 the original author or authors.
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

package com.proit.app.report.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("app.report")
public class ReportProperties
{
	public static final String ACTIVATION_PROPERTY = "app.report.enabled";
	public static final String STORE_TYPE_PROPERTY = "app.report.store-type";

	public static final String STORE_TYPE_MEMORY = "memory";
	public static final String STORE_TYPE_ATTACHMENT = "attachment";

	private boolean enabled = true;

	private String storeType;
}