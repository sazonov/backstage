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

package com.proit.app.service.query.ast;

import com.proit.app.model.other.date.DateConstants;
import com.proit.app.service.query.TranslationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public class Constant implements QueryOperand
{
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
					this.dateValue = LocalDate.parse(value, DateConstants.UNIVERSAL_DATE_FORMATTER);
					this.type = Type.DATE;
					this.dateTimeValue = null;
				}

				case "timestamp" -> {
					this.dateTimeValue = LocalDateTime.parse(value, DateConstants.UNIVERSAL_DATE_TIME_FORMATTER);
					this.type = Type.TIMESTAMP;
					this.dateValue = null;
				}

				default -> throw new RuntimeException("supported types for cast: date, timestamp");
			}
		}
		catch (DateTimeParseException e)
		{
			throw new RuntimeException("supported date patterns: %s".formatted(DateConstants.AVAILABLE_DATE_PATTERNS), e);
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
