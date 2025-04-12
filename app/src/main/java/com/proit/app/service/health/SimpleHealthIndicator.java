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

package com.proit.app.service.health;

import org.springframework.boot.actuate.health.Health;

import java.time.LocalDateTime;

public class SimpleHealthIndicator extends AbstractHealthIndicator
{
	private Boolean up;

	private String message;

	private Exception exception;

	protected void up()
	{
		this.up = true;
		this.message = null;
		this.exception = null;
		this.lastUpdate = LocalDateTime.now();
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
	}
}
