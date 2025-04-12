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

package com.proit.app.utils;

import com.proit.app.exception.AppException;
import com.proit.app.model.other.date.DateConstants;
import com.proit.app.model.other.exception.ApiStatusCodeImpl;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@UtilityClass
public class DateUtils
{
	public static final Locale DEFAULT_LOCALE = new Locale("ru");

	/**
	 * LocalTime
	 */
	public static LocalTime toLocalTime(Date date)
	{
		return Optional.ofNullable(date)
				.map(DateUtils::toLocalDateTime)
				.map(LocalDateTime::toLocalTime)
				.orElse(null);
	}

	/**
	 * LocalDate
	 */
	public static LocalDate toLocalDate(ZonedDateTime dateTime)
	{
		return Optional.ofNullable(dateTime)
				.map(date -> date.withZoneSameInstant(ZoneId.systemDefault()))
				.map(ZonedDateTime::toLocalDate)
				.orElse(null);
	}

	public static LocalDate toLocalDate(Date date)
	{
		return Optional.ofNullable(date)
				.map(DateUtils::toLocalDateTime)
				.map(LocalDateTime::toLocalDate)
				.orElse(null);
	}

	public static LocalDate toLocalDateOrNow(String date)
	{
		return toLocalDateOr(date, LocalDate.now());
	}

	public static LocalDate toLocalDateOr(String date, LocalDate defaultValue)
	{
		return toLocalDateOr(date, defaultValue, DateConstants.ISO_DATE_FORMATTER);
	}

	public static LocalDate toLocalDateOrNow(String date, DateTimeFormatter format)
	{
		return toLocalDateOr(date, LocalDate.now(), format);
	}

	public static LocalDate toLocalDateOr(String date, LocalDate defaultValue, DateTimeFormatter format)
	{
		try
		{
			return Optional.ofNullable(date)
					.map(d -> LocalDate.parse(d, format))
					.orElse(defaultValue);
		}
		catch (DateTimeParseException e)
		{
			throw new AppException(ApiStatusCodeImpl.DATE_PARSE_ERROR, e);
		}
	}

	public static LocalDate toLocalDate(String date)
	{
		return toLocalDateOr(date, null, DateTimeFormatter.ISO_LOCAL_DATE);
	}

	/**
	 * LocalDateTime
	 * Для конвертации в LocalDateTime используем epoch, т.к. в наследнике java.util.Date
	 * переопределен метод toInstant()
	 * <pre>{@code
	 *  @Override
	 * 	public Instant toInstant() {
	 * 	    throw new java.lang.UnsupportedOperationException();
	 * 	}
	 * }</pre>
	 * @see java.sql.Date
	 */
	public static LocalDateTime toLocalDateTime(Date date)
	{
		return Optional.ofNullable(date)
				.map(it -> fromEpochMilli(date.getTime()))
				.orElse(null);
	}

	public static LocalDateTime toLocalDateTime(ZonedDateTime dateTime)
	{
		return Optional.ofNullable(dateTime)
				.map(date -> date.withZoneSameInstant(ZoneId.systemDefault()))
				.map(ZonedDateTime::toLocalDateTime)
				.orElse(null);
	}

	public static LocalDateTime toLocalDateTime(XMLGregorianCalendar gregorianCalendar)
	{
		return Optional.ofNullable(gregorianCalendar)
				.map(XMLGregorianCalendar::toGregorianCalendar)
				.map(GregorianCalendar::toZonedDateTime)
				.map(DateUtils::toLocalDateTime)
				.orElse(null);
	}

	public static LocalDateTime toLocalDateTime(String dateTime)
	{
		return toLocalDateTime(dateTime, DateConstants.ISO_DATE_TIME_SECONDS_FORMATTER);
	}

	public static LocalDateTime toLocalDateTime(String dateTime, DateTimeFormatter format)
	{
		try
		{
			return Optional.ofNullable(dateTime)
					.filter(StringUtils::isNotBlank)
					.map(it -> LocalDateTime.parse(it, format))
					.orElse(null);
		}
		catch (DateTimeParseException e)
		{
			throw new AppException(ApiStatusCodeImpl.DATE_PARSE_ERROR, e);
		}
	}

	public static LocalDateTime fromEpochMilli(Long time)
	{
		return Optional.ofNullable(time)
				.map(t -> LocalDateTime.ofInstant(Instant.ofEpochMilli(t), ZoneId.systemDefault()))
				.orElse(null);
	}

	public static LocalDateTime fromEpochSecond(Long time)
	{
		return Optional.ofNullable(time)
				.map(t -> fromEpochMilli(t * 1000))
				.orElse(null);
	}

	/**
	 * ZonedDateTime
	 */
	public static ZonedDateTime toZonedDateTime(Date date)
	{
		return Optional.ofNullable(date)
				.map(d -> ZonedDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault()))
				.orElse(null);
	}

	public static ZonedDateTime toZonedDateTime(LocalDateTime dateTime)
	{
		return Optional.ofNullable(dateTime)
				.map(date -> date.atZone(ZoneId.systemDefault()))
				.orElse(null);
	}

	public static ZonedDateTime toUtcZonedDateTime(Date date)
	{
		return ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
	}

	/**
	 * Date
	 */
	public static Date toDate(ZonedDateTime zonedDateTime)
	{
		return Optional.ofNullable(zonedDateTime)
				.map(date -> Date.from(date.toInstant()))
				.orElse(null);
	}

	public static Date toDate(LocalDateTime localDateTime)
	{
		return Optional.ofNullable(localDateTime)
				.map(date -> Date.from(date.atZone(ZoneId.systemDefault()).toInstant()))
				.orElse(null);
	}

	public static Date toDate(XMLGregorianCalendar xmlCalendar)
	{
		return xmlCalendar.toGregorianCalendar().getTime();
	}

	public static Date toDateWithMinTime(LocalDate localDate)
	{
		return toDate(LocalDateTime.of(localDate, LocalTime.MIN));
	}

	public static Date toDateWithMinTime(Date date)
	{
		return toDateWithMinTime(toLocalDate(date));
	}

	public static Date toDateWithMaxTime(LocalDate localDate)
	{
		return toDate(LocalDateTime.of(localDate, LocalTime.MAX));
	}

	public static Date toDateWithMaxTime(Date date)
	{
		return toDateWithMaxTime(toLocalDate(date));
	}

	public static Date toDateEx(String date, String pattern)
	{
			return Optional.ofNullable(pattern)
					.map(DateTimeFormatter::ofPattern)
					.map(formatter -> formatter.parse(date))
					.map(Instant::from)
					.map(Date::from)
					.orElseThrow();
	}

	public static Date toDate(String date)
	{
		return toDate(date, DateConstants.ISO_DATE_TIME_SECONDS_FORMAT);
	}

	public static Date toDate(String date, String format)
	{
		return Optional.ofNullable(date)
				.map(it -> toDateEx(it, format))
				.orElse(null);
	}

	public static List<Date> getDateInterval(Date from, int days)
	{
		return IntStream.range(0, days)
				.mapToObj(i -> DateUtils.toDateWithMinTime(DateUtils.toLocalDate(from).plusDays(i)))
				.toList();
	}

	/**
	 * Month
	 */
	public static String getLocalMonthName(int monthNumber)
	{
		return getLocalMonthName(monthNumber, TextStyle.FULL_STANDALONE, DEFAULT_LOCALE);
	}

	public static String getLocalMonthName(int monthNumber, @NonNull TextStyle textStyle, @NonNull Locale locale)
	{
		if (monthNumber < 1 || monthNumber > 12)
		{
			throw new AppException(ApiStatusCodeImpl.ILLEGAL_INPUT);
		}

		var month = Month.values()[monthNumber - 1];
		var localMonthName = month.getDisplayName(textStyle, locale);

		return StringUtils.capitalize(localMonthName);
	}

	public static Map<Long, String> getLocalMonthNames()
	{
		return Arrays.stream(Month.values())
				.map(month -> Map.entry((long) month.getValue(), month.getDisplayName(TextStyle.FULL_STANDALONE, DEFAULT_LOCALE)))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	/**
	 * Other
	 */
	public static long toEpoch(LocalDateTime localDateTime)
	{
		return toZonedDateTime(localDateTime).toEpochSecond();
	}

	public static boolean isDayBeforeOrEqual(Date date1, Date date2)
	{
		return org.apache.commons.lang3.time.DateUtils.isSameDay(date1, date2) || date1.before(date2);
	}

	public static boolean isDayBetween(Date date, Date from, Date to)
	{
		return date.after(from) && isDayBeforeOrEqual(date, to);
	}

	public static void checkDateString(String date, String pattern)
	{
		checkDateString(date, DateTimeFormatter.ofPattern(pattern));
	}

	public static void checkDateString(String date, DateTimeFormatter formatter)
	{
		formatter.parse(date);
	}

	public static LocalDate getCurrentWeekLastDate()
	{
		var localDate = LocalDate.now();

		return localDate.plusDays(6L - localDate.getDayOfWeek().ordinal());
	}

	public static LocalDateTime getQuarterStart(LocalDateTime date)
	{
		return date.toLocalDate()
				.with(date.getMonth().firstMonthOfQuarter())
				.with(TemporalAdjusters.firstDayOfMonth())
				.atStartOfDay();
	}

	public static Date getQuarterStart(Date date)
	{
		final Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) / 3 * 3);

		return calendar.getTime();
	}
}
