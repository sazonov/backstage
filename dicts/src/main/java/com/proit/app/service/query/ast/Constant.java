/*
 *    Copyright 2019-2022 the original author or authors.
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

import com.proit.app.model.api.ApiConstants;
import com.proit.app.service.query.TranslationContext;
import org.apache.commons.lang3.time.DateUtils;

import java.util.Arrays;
import java.util.Date;

public class Constant implements Operand
{
	private static final String[] DATE_PATTERNS = { ApiConstants.API_TIMESTAMP_FORMAT, "yyyy-MM-dd'T'HH:mm:ssXXX", ApiConstants.API_DATE_FORMAT };

	public enum Type
	{
		INTEGER,
		STRING,
		BOOLEAN,
		TIMESTAMP,
		NULL
	}

	public final Type type;
	public final String value;
	public final Date dateValue;

	public Constant(String value, Type type)
	{
		this.value = value;
		this.type = type;
		this.dateValue = null;
	}

	public Constant(Constant source, String targetType)
	{
		if (!targetType.equals("date"))
		{
			throw new RuntimeException("supported types for cast: date, timestamp");
		}

		this.value = source.value;
		this.type = Type.TIMESTAMP;

		try
		{
			this.dateValue = DateUtils.parseDate(value, DATE_PATTERNS);
		}
		catch (Exception e)
		{
			throw new RuntimeException("supported date patterns: %s".formatted(Arrays.asList(DATE_PATTERNS)));
		}
	}

	public Object getValue()
	{
		if (value == null)
		{
			return null;
		}

		Object result;

		switch (type)
		{
			case INTEGER -> result = Long.parseLong(value);
			case STRING -> result = value;
			case BOOLEAN -> result = Boolean.parseBoolean(value);
			case TIMESTAMP -> result = dateValue;

			default -> result = null;
		}

		return result;
	}

	public <T> T process(Visitor<T> visitor, TranslationContext context)
	{
		return visitor.visit(this, context);
	}
}
