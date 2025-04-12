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

package com.proit.app.database.configuration.ddl;

import com.proit.app.database.configuration.properties.DDLProperties;
import com.proit.app.database.utils.DDLUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;

@Getter
@RequiredArgsConstructor
public abstract class AbstractDDLProvider implements DDLProvider
{
	private final String appName;

	private final DDLProperties properties;

	private final DataSource dataSource;

	public AbstractDDLProvider(String appName, String scheme, DataSource dataSource)
	{
		this.appName = appName;
		this.dataSource = dataSource;
		this.properties = new DDLProperties(true, scheme);
	}

	public void update()
	{
		if (properties.isEnabled())
		{
			DDLUtils.applyDDL(getAppName(), properties.getScheme(), getDataSource());
		}
	}
}
