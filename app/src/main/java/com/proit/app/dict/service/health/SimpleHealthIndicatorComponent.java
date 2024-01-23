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

import lombok.Getter;
import org.springframework.boot.actuate.health.Health;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
public class SimpleHealthIndicatorComponent extends AbstractHealthIndicatorComponent
{
	private Boolean up;

	private String message;

	private Exception exception;

	private final Map<String, Object> details = new HashMap<>();

	protected void up()
	{
		this.up = true;
		this.message = null;
		this.exception = null;
		this.lastUpdate = LocalDateTime.now();
		this.details.clear();
	}

	protected void upWithDetails(Map<String, Object> details)
	{
		up();

		this.details.putAll(details);
	}

	protected void down()
	{
		down(null, null);
	}

	protected void down(String message)
	{
		down(message, null);
	}

	protected void down(Exception exception)
	{
		down(null, exception);
	}

	private void down(String message, Exception exception)
	{
		this.up = false;
		this.message = message;
		this.exception = exception;
		this.lastUpdate = LocalDateTime.now();
		this.details.clear();
	}

	protected void downWithDetails(Map<String, Object> details)
	{
		down(null, null);

		this.details.putAll(details);
	}

	protected void downWithDetails(Exception exception, Map<String, Object> details)
	{
		down(null, exception);

		this.details.putAll(details);
	}

	protected void downWithDetails(String message, Map<String, Object> details)
	{
		down(message, null);

		this.details.putAll(details);
	}

	protected void doHealthCheck(Health.Builder builder)
	{
		if (up == null)
		{
			builder.unknown();
		}
		else
		{
			if (up)
			{
				builder.up();
			}
			else
			{
				if (exception != null)
				{
					builder.down().withException(exception);
				}
				else if (message != null)
				{
					builder.down().withDetail("description", message);
				}
				else
				{
					builder.down();
				}
			}
		}

		builder.withDetails(details);
	}
}
