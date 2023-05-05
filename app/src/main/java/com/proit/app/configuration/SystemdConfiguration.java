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

import com.github.jpmsilva.jsystemd.ConditionalOnSystemd;
import com.github.jpmsilva.jsystemd.SystemdNotifyStatusProvider;
import com.proit.app.configuration.properties.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

import javax.annotation.Nonnull;
import java.util.stream.Collectors;

@Configuration
@ConditionalOnSystemd
@ConditionalOnProperty(name = "enabled", prefix = "systemd.health-provider")
public class SystemdConfiguration
{
	// Возвращает информацию о загруженных конфигах
	@Bean
	public SystemdNotifyStatusProvider configStatus(ConfigurableEnvironment env)
	{
		return new SystemdNotifyStatusProvider() {
			@Override
			public @Nonnull String status()
			{
				return env.getPropertySources().stream()
						.map(PropertySource::getName)
						.filter(name -> name.contains(".properties") || name.contains(".yml"))
						.map(source -> source.substring(source.indexOf("[") + 1, source.indexOf("]")))
						.collect(Collectors.joining(",", "Configs [", "]"));
			}
		};
	}

	// Возвращает информацию о версии билда
	@Bean
	public SystemdNotifyStatusProvider versionStatus(AppProperties appProperties)
	{
		return new SystemdNotifyStatusProvider() {
			@Override
			public @Nonnull String status()
			{
				return "Build " + appProperties.getVersion();
			}
		};
	}
}
