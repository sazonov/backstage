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

package com.proit.bpm.service.jbpm;

import com.proit.app.configuration.properties.BusinessCalendarProperties;
import com.proit.app.service.BusinessCalendarService;
import lombok.RequiredArgsConstructor;
import org.jbpm.process.core.timer.BusinessCalendar;
import org.jbpm.process.core.timer.DateTimeUtils;
import org.jbpm.util.PatternConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.regex.Matcher;

@Component
@ConditionalOnProperty(BusinessCalendarProperties.ACTIVATING_PROPERTY)
@RequiredArgsConstructor
public class ProcessCalendar implements BusinessCalendar
{
	private static final int SIM_WEEK = 3;
	private static final int SIM_DAY = 5;
	private static final int SIM_HOU = 7;
	private static final int SIM_MIN = 9;
	private static final int SIM_SEC = 11;

	private final BusinessCalendarService businessCalendarService;

	public long calculateBusinessTimeAsDuration(String timeExpression)
	{
		return calculateBusinessTimeAsDate(timeExpression).getTime() - new Date().getTime();
	}

	public Date calculateBusinessTimeAsDate(String timeExpression)
	{
		timeExpression = adoptISOFormat(timeExpression);

		String trimmed = timeExpression.trim();
		int weeks = 0;
		int days = 0;
		int hours = 0;
		int min = 0;
		int sec = 0;

		if (trimmed.length() > 0)
		{
			Matcher mat = PatternConstants.SIMPLE_TIME_DATE_MATCHER.matcher(trimmed);
			if (mat.matches())
			{
				weeks = (mat.group(SIM_WEEK) != null) ? Integer.parseInt(mat.group(SIM_WEEK)) : 0;
				days = (mat.group(SIM_DAY) != null) ? Integer.parseInt(mat.group(SIM_DAY)) : 0;
				hours = (mat.group(SIM_HOU) != null) ? Integer.parseInt(mat.group(SIM_HOU)) : 0;
				min = (mat.group(SIM_MIN) != null) ? Integer.parseInt(mat.group(SIM_MIN)) : 0;
				sec = (mat.group(SIM_SEC) != null) ? Integer.parseInt(mat.group(SIM_SEC)) : 0;
			}
		}

		int totalMinutes = ((weeks * 7 + days) * 24 + hours) * 60 + min;

		return Date.from(businessCalendarService.addBusinessMinutes(LocalDateTime.now(), totalMinutes).atZone(ZoneId.systemDefault()).toInstant());
	}

	protected String adoptISOFormat(String timeExpression)
	{
		try
		{
			Duration p = null;

			if (DateTimeUtils.isPeriod(timeExpression))
			{
				p = Duration.parse(timeExpression);
			}
			else if (DateTimeUtils.isNumeric(timeExpression))
			{
				p = Duration.of(Long.valueOf(timeExpression), ChronoUnit.MILLIS);
			}
			else
			{
				OffsetDateTime dateTime = OffsetDateTime.parse(timeExpression, DateTimeFormatter.ISO_DATE_TIME);
				p = Duration.between(OffsetDateTime.now(), dateTime);
			}

			long days = p.toDays();
			long hours = p.toHours() % 24;
			long minutes = p.toMinutes() % 60;
			long seconds = p.getSeconds() % 60;
			long milis = p.toMillis() % 1000;

			StringBuffer time = new StringBuffer();
			if (days > 0)
			{
				time.append(days + "d");
			}
			if (hours > 0)
			{
				time.append(hours + "h");
			}
			if (minutes > 0)
			{
				time.append(minutes + "m");
			}
			if (seconds > 0)
			{
				time.append(seconds + "s");
			}
			if (milis > 0)
			{
				time.append(milis + "ms");
			}

			return time.toString();
		}
		catch (Exception e)
		{
			return timeExpression;
		}
	}
}
