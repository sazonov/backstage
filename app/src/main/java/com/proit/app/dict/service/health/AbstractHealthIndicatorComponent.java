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

package com.proit.app.dict.service.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.time.LocalDateTime;

@Slf4j
public abstract class AbstractHealthIndicatorComponent
{
	protected LocalDateTime lastUpdate = LocalDateTime.now();

	public Health health()
	{
		Health.Builder builder = new Health.Builder();

		try
		{
			doHealthCheck(builder);

			builder.withDetail("lastUpdate", lastUpdate);
		}
		catch (Exception ex)
		{
			log.warn("Health check failed.", ex);

			builder.down(ex);
		}

		return builder.build();
	}

	protected abstract void doHealthCheck(Health.Builder builder) throws Exception;

	public HealthIndicator asHealthIndicator()
	{
		return this::health;
	}
}
