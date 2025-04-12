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

package com.proit.app.report.utils;

import lombok.experimental.UtilityClass;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class ReportDateUtils
{
	public static final String DATE_TIME_PATTERN = "dd.MM.yyyy, HH:mm:ss";
	public static final String DATE_PATTERN = "dd.MM.yyyy";
	public static final String TIME_PATTERN = "HH:mm:ss";
	public static final String YEAR_MONTH_PATTERN = "yyyy.MM";

	public static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
	public static final DateTimeFormatter LOCAL_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
	public static final DateTimeFormatter LOCAL_DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
	public static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat(DATE_TIME_PATTERN);
	public static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat(TIME_PATTERN);
	public static final SimpleDateFormat YEAR_MONTH_FORMATTER = new SimpleDateFormat(YEAR_MONTH_PATTERN);
}
