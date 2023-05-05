/*
 *
 *  Copyright 2019-2023 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.proit.app.model.other.date;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;

public class DateConstants
{
	public static final String API_TIME_FORMAT = "HH:mm:ss";
	public static final String API_DATE_FORMAT = "yyyy-MM-dd";
	public static final String API_ZONED_DATE_FORMAT = "yyyy-MM-ddXXX";
	public static final String API_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	public static final String API_ZONED_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
	public static final String API_DATE_TIME_MILLIS_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	public static final String API_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

	public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(API_DATE_FORMAT);
	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(API_TIMESTAMP_FORMAT);
	public static final DateTimeFormatter UNIVERSAL_DATE_FORMATTER = new DateTimeFormatterBuilder()
			.appendOptional(DateTimeFormatter.ISO_DATE_TIME)
			.appendOptional(DateTimeFormatter.ISO_DATE)
			.toFormatter();

	public static final DateTimeFormatter UNIVERSAL_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
			.appendOptional(DateTimeFormatter.ISO_DATE_TIME)
			.appendOptional(DateTimeFormatter.ISO_DATE)
			.parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
			.parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
			.parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
			.toFormatter();

	public static final List<String> AVAILABLE_DATE_PATTERNS = List.of(API_DATE_FORMAT, API_ZONED_DATE_FORMAT, API_DATE_TIME_FORMAT, API_ZONED_DATE_TIME_FORMAT, API_DATE_TIME_MILLIS_FORMAT, API_TIMESTAMP_FORMAT);
}
