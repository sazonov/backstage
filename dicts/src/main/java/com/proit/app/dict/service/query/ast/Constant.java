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

package com.proit.app.dict.service.query.ast;

import com.proit.app.dict.service.query.TranslationContext;
import com.proit.app.model.other.date.DateConstants;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.List;

public class Constant implements QueryOperand
{
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

	public static final List<String> AVAILABLE_DATE_PATTERNS = List.of(
			DateConstants.ISO_DATE_FORMAT,
			DateConstants.ISO_OFFSET_DATE_FORMAT,
			DateConstants.ISO_DATE_TIME_SECONDS_FORMAT,
			DateConstants.ISO_OFFSET_DATE_TIME_SECONDS_FORMAT,
			DateConstants.ISO_DATE_TIME_MS_FORMAT,
			DateConstants.ISO_OFFSET_DATE_TIME_MS_FORMAT);

	public enum Type
	{
		INTEGER,
		DECIMAL,
		STRING,
		BOOLEAN,
		DATE,
		TIMESTAMP,
		NULL
	}

	public final Type type;
	public final String value;
	public final LocalDate dateValue;
	public final LocalDateTime dateTimeValue;

	public Constant(String value, Type type)
	{
		this.value = value;
		this.type = type;
		this.dateValue = null;
		this.dateTimeValue = null;
	}

	public Constant(Constant source, String targetType)
	{
		this.value = source.value;

		try
		{
			switch (targetType)
			{
				case "date" -> {
					this.dateValue = LocalDate.parse(value, UNIVERSAL_DATE_FORMATTER);
					this.type = Type.DATE;
					this.dateTimeValue = null;
				}

				case "timestamp" -> {
					this.dateTimeValue = LocalDateTime.parse(value, UNIVERSAL_DATE_TIME_FORMATTER);
					this.type = Type.TIMESTAMP;
					this.dateValue = null;
				}

				default -> throw new RuntimeException("supported types for cast: date, timestamp");
			}
		}
		catch (DateTimeParseException e)
		{
			throw new RuntimeException("supported date patterns: %s".formatted(AVAILABLE_DATE_PATTERNS), e);
		}
	}

	public Object getValue()
	{
		if (value == null)
		{
			return null;
		}

		return switch (type)
		{
			case INTEGER -> Long.parseLong(value);
			case DECIMAL -> BigDecimal.valueOf(Double.parseDouble(value))
					.stripTrailingZeros()
					.toPlainString();
			case STRING -> value;
			case BOOLEAN -> Boolean.parseBoolean(value);
			case DATE -> dateValue;
			case TIMESTAMP -> dateTimeValue;
			case NULL -> null;

			default -> throw new RuntimeException("unsupported type: %s".formatted(type));
		};
	}

	public <T> T process(Visitor<T> visitor, TranslationContext context)
	{
		return visitor.visit(this, context);
	}
}
