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

package com.proit.app.jobs.model.dto;

import com.proit.app.exception.AppException;
import com.proit.app.model.other.exception.ApiStatusCodeImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.util.function.Function;

@RequiredArgsConstructor
public enum JobTriggerType
{
	CRON(expression -> {
		if (!CronExpression.isValidExpression(expression))
		{
			throw new AppException(ApiStatusCodeImpl.ILLEGAL_INPUT);
		}

		return new CronTrigger(expression);
	}),

	INTERVAL(expression -> {
		try
		{
			var fixedDelay = Long.parseLong(expression);

			return new PeriodicTrigger(fixedDelay);
		}
		catch (NumberFormatException e)
		{
			throw new AppException(ApiStatusCodeImpl.ILLEGAL_INPUT, e);
		}
	});

	private final Function<String, Trigger> expressionParser;

	public Trigger createTrigger(String expression)
	{
		return expressionParser.apply(expression);
	}
}
