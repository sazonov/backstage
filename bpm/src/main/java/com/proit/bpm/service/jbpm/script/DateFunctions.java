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

package com.proit.bpm.service.jbpm.script;

import com.proit.app.model.other.date.DateConstants;
import com.proit.app.service.BusinessCalendarService;
import com.proit.bpm.exception.BpmException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Component
public class DateFunctions implements ScriptingExtension, ApplicationContextAware
{
	private BusinessCalendarService businessCalendarService;

	public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException
	{
		try
		{
			businessCalendarService = applicationContext.getBean(BusinessCalendarService.class);
		}
		catch (Exception ignore)
		{
			// no handling
		}
	}

	/**
	 * @deprecated новый метод {@link #addDays(int)}.
	 */
	@Deprecated
	public Date shiftDaysFromCurrentDate(int days)
	{
		log.warn("You are using deprecated method.");

		return addDays(days);
	}

	public Date addDays(int days)
	{
		return addDays(new Date(), days);
	}

	public Date addDays(Date date, int days)
	{
		return DateUtils.addDays(date, days);
	}

	public Date addBusinessDays(int days)
	{
		return addBusinessDays(new Date(), days);
	}

	public Date addBusinessDays(Date date, int days)
	{
		if (businessCalendarService == null)
		{
			throw new BpmException("business calendar is not available");
		}

		var localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());

		return Date.from(businessCalendarService.addBusinessDays(localDateTime, days).atZone(ZoneId.systemDefault()).toInstant());
	}

	public String formatToTimestamp(Date date)
	{
		return DateFormatUtils.format(date, DateConstants.API_TIMESTAMP_FORMAT);
	}
}
