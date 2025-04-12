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

package com.proit.app.model.other.date;

import java.time.format.DateTimeFormatter;

public class DateConstants
{
	public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";
	public static final String ISO_TIME_SECONDS_FORMAT = "HH:mm:ss";
	public static final String ISO_TIME_MINUTES_FORMAT = "HH:mm";

	/**
	 * yyyy-MM-ddXXX
	 */
	public static final String ISO_OFFSET_DATE_FORMAT = ISO_DATE_FORMAT + "XXX";

	/**
	 * yyyy-MM-dd'T'HH:mm:ss
	 */
	public static final String ISO_DATE_TIME_SECONDS_FORMAT = ISO_DATE_FORMAT + "'T'" + ISO_TIME_SECONDS_FORMAT;

	/**
	 * yyyy-MM-dd'T'HH:mm:ss.SSS
	 */
	public static final String ISO_DATE_TIME_MS_FORMAT = ISO_DATE_TIME_SECONDS_FORMAT + ".SSS";

	/**
	 * yyyy-MM-dd'T'HH:mm:ss.SSSXXX
	 */
	public static final String ISO_OFFSET_DATE_TIME_MS_FORMAT = ISO_DATE_TIME_SECONDS_FORMAT + ".SSSXXX";

	/**
	 * yyyy-MM-dd'T'HH:mm:ssXXX
	 */
	public static final String ISO_OFFSET_DATE_TIME_SECONDS_FORMAT = ISO_DATE_TIME_SECONDS_FORMAT + "XXX";

	/**
	 * yyyy-MM-dd HH:mm:ss
	 */
	public static final String SIMPLE_DATE_TIME_SECONDS_FORMAT = ISO_DATE_FORMAT + " " + ISO_TIME_SECONDS_FORMAT;

	/**
	 * yyyy-MM-dd HH:mm
	 */
	public static final String SIMPLE_DATE_TIME_MINUTES_FORMAT = ISO_DATE_FORMAT + " " + ISO_TIME_MINUTES_FORMAT;

	public static final String RUSSIAN_DATE_FORMAT = "dd.MM.yyyy";

	/**
	 * dd.MM.yyyy HH:mm
	 */
	public static final String RUSSIAN_DATE_TIME_MINUTES_FORMAT = RUSSIAN_DATE_FORMAT + " " + ISO_TIME_MINUTES_FORMAT;

	/**
	 * dd.MM.yyyy HH:mm:ss
	 */
	public static final String RUSSIAN_DATE_TIME_SECONDS_FORMAT = RUSSIAN_DATE_FORMAT + " " + ISO_TIME_SECONDS_FORMAT;

	/**
	 * HH:mm dd.MM.yyyy
	 */
	public static final String INVERT_RUSSIAN_DATE_TIME_FORMAT = ISO_TIME_MINUTES_FORMAT + " " + RUSSIAN_DATE_FORMAT;

	public static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern(ISO_DATE_FORMAT);
	public static final DateTimeFormatter ISO_OFFSET_DATE_TIME_MS_FORMATTER = DateTimeFormatter.ofPattern(ISO_OFFSET_DATE_TIME_MS_FORMAT);
	public static final DateTimeFormatter ISO_TIME_MINUTES_FORMATTER = DateTimeFormatter.ofPattern(ISO_TIME_MINUTES_FORMAT);
	public static final DateTimeFormatter ISO_DATE_TIME_SECONDS_FORMATTER = DateTimeFormatter.ofPattern(ISO_DATE_TIME_SECONDS_FORMAT);

	public static final DateTimeFormatter SIMPLE_DATE_TIME_SECONDS_FORMATTER = DateTimeFormatter.ofPattern(SIMPLE_DATE_TIME_SECONDS_FORMAT);
	public static final DateTimeFormatter SIMPLE_DATE_TIME_MINUTES_FORMATTER = DateTimeFormatter.ofPattern(SIMPLE_DATE_TIME_MINUTES_FORMAT);

	public static final DateTimeFormatter RUSSIAN_DATE_FORMATTER = DateTimeFormatter.ofPattern(RUSSIAN_DATE_FORMAT);
	public static final DateTimeFormatter RUSSIAN_DATE_TIME_MINUTES_FORMATTER = DateTimeFormatter.ofPattern(RUSSIAN_DATE_TIME_MINUTES_FORMAT);
	public static final DateTimeFormatter RUSSIAN_DATE_TIME_SECONDS_FORMATTER = DateTimeFormatter.ofPattern(RUSSIAN_DATE_TIME_SECONDS_FORMAT);
	public static final DateTimeFormatter INVERT_RUSSIAN_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(INVERT_RUSSIAN_DATE_TIME_FORMAT);
}
