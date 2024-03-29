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

package com.proit.app.database.configuration.ddl;

import com.proit.app.database.configuration.properties.DDLProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@ConditionalOnProperty(value = DDLProperties.ACTIVATION_PROPERTY, matchIfMissing = true)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BaseDDLProvider extends AbstractDDLProvider implements SystemDDLProvider
{
	@Autowired
	public BaseDDLProvider(@Qualifier("ddlDataSource") DataSource dataSource)
	{
		super("essentials", dataSource);
	}
}
