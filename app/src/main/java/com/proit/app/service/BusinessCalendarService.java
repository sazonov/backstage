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

package com.proit.app.service;

import com.proit.app.configuration.properties.BusinessCalendarProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@ConditionalOnProperty(BusinessCalendarProperties.ACTIVATING_PROPERTY)
@EnableConfigurationProperties(BusinessCalendarProperties.class)
@RequiredArgsConstructor
public class BusinessCalendarService
{
	private final BusinessCalendarProperties businessCalendarProperties;

	/**
	 * Добавляет к указанной дате рабочие дни.
	 */
	public LocalDate addBusinessDays(LocalDate date, int days)
	{
		while (days > 0)
		{
			date = date.plusDays(1);

			if (isBusinessDay(date))
			{
				days--;
			}
		}

		return date;
	}

	/**
	 * Добавляет к указанной дате и времени рабочие дни.
	 */
	public LocalDateTime addBusinessDays(LocalDateTime date, int days)
	{
		return date.with(addBusinessDays(date.toLocalDate(), days));
	}

	/**
	 * Добавляет к указанной дате и времени рабочие часы.
	 */
	public LocalDateTime addBusinessHours(LocalDateTime dateTime, int hours)
	{
		int hoursInDay = businessCalendarProperties.getDayEnd().getHour() - businessCalendarProperties.getDayStart().getHour();

		dateTime = dateTime.with(addBusinessDays(dateTime, hours / hoursInDay));

		hours = hours % hoursInDay;

		while (hours > 0)
		{
			dateTime = getNearBusinessDateTime(dateTime.plusHours(1));

			hours--;
		}

		return dateTime;
	}

	/**
	 * Добавляет к указанной дате и времени рабочие минуты.
	 */
	public LocalDateTime addBusinessMinutes(LocalDateTime dateTime, int minutes)
	{
		dateTime = addBusinessHours(dateTime, minutes / 60);

		minutes = minutes % 60;

		while (minutes > 0)
		{
			dateTime = getNearBusinessDateTime(dateTime.plusMinutes(1));

			minutes--;
		}

		return dateTime;
	}

	/**
	 * Возвращает ближайший к указанной дате рабочий день. Если передан рабочий день, то возвращает дату без изменений.
	 */
	public LocalDate getNearBusinessDate(LocalDate date)
	{
		while (!isBusinessDay(date))
		{
			date = date.plusDays(1);
		}

		return date;
	}

	/**
	 * Возвращает ближайший к указанной дате рабочий день с временем. Корректирует и дату и время. Если указанные день и время
	 * являются рабочими, то возвращает переданную дату без изменений.
	 */
	public LocalDateTime getNearBusinessDateTime(LocalDateTime dateTime)
	{
		if (!isBusinessTime(dateTime.toLocalTime()))
		{
			var time = dateTime.toLocalTime();

			if (time.isAfter(businessCalendarProperties.getDayEnd()) || time.equals(businessCalendarProperties.getDayEnd()))
			{
				dateTime = dateTime.plusDays(1);
			}

			dateTime = dateTime.with(businessCalendarProperties.getDayStart());
		}

		return dateTime.with(getNearBusinessDate(dateTime.toLocalDate()));
	}

	/**
	 * Проверяет, является ли день рабочим.
	 */
	public boolean isBusinessDay(LocalDate date)
	{
		return !businessCalendarProperties.getVacations().contains(date) && (
				businessCalendarProperties.getWorkDays().contains(date.getDayOfWeek()) || businessCalendarProperties.getExtraWorkDays().contains(date));
	}

	/**
	 * Проверяет, является ли время рабочим.
	 */
	public boolean isBusinessTime(LocalTime time)
	{
		return (time.isAfter(businessCalendarProperties.getDayStart()) && time.isBefore(businessCalendarProperties.getDayEnd())) || time.equals(businessCalendarProperties.getDayStart());
	}
}
