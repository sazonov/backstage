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

package com.proit.app.configuration;

import com.proit.app.configuration.properties.ApiProperties;
import com.proit.app.configuration.properties.AppProperties;
import com.proit.app.service.user.PermissionService;
import com.proit.app.utils.SecurityUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties({AppProperties.class, ApiProperties.class})
public class AppConfiguration
{
	@Bean
	@ConditionalOnMissingBean
	public PermissionService noOpPermissionService()
	{
		return new PermissionService() {
			@Override
			public List<String> getPermissions(String userId)
			{
				return List.of();
			}

			@Override
			public List<String> getPermissions()
			{
				return getPermissions(SecurityUtils.getCurrentUserId());
			}
		};
	}
}
