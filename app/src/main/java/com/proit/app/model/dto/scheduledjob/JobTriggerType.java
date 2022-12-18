package com.proit.app.model.dto.scheduledjob;

import com.proit.app.exception.AppException;
import com.proit.app.model.api.ApiStatusCodeImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.util.function.Function;

@RequiredArgsConstructor
public enum JobTriggerType
{
	CRON(expression ->
	{
		if (!CronExpression.isValidExpression(expression))
		{
			throw new AppException(ApiStatusCodeImpl.ILLEGAL_INPUT);
		}

		return new CronTrigger(expression);
	}),

	INTERVAL(expression ->
	{
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
