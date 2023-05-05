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

package com.proit.bpm.configuration;

import com.proit.app.configuration.ddl.AbstractDDLProvider;
import com.proit.app.configuration.ddl.DDLConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;

@Configuration
@Order(BpmDDLProvider.PRECEDENCE)
public class BpmDDLProvider extends AbstractDDLProvider
{
	public static final int PRECEDENCE = DDLConfiguration.DDL_PRECEDENCE_SYSTEM + 60;

	@Autowired
	public BpmDDLProvider(@Qualifier("noTimeoutDataSource") DataSource dataSource)
	{
		super("bpm", dataSource);
	}
}
