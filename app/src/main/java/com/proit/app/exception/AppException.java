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

package com.proit.app.exception;

import com.proit.app.model.api.ApiStatusCode;
import lombok.Getter;
import org.springframework.validation.Errors;

import java.util.HashMap;
import java.util.Map;

@Getter
public class AppException extends RuntimeException
{
	private final ApiStatusCode status;

	private Map<String, Object> data;

	private Errors validationErrors;

	public AppException(ApiStatusCode status)
	{
		super(status.getMessage());

		this.status = status;
	}

	public AppException(ApiStatusCode status, String message)
	{
		super(message);

		this.status = status;
	}

	public AppException(ApiStatusCode status, Throwable cause)
	{
		super(status.getMessage(), cause);

		this.status = status;
	}

	public AppException(ApiStatusCode status, String message, Throwable cause)
	{
		super(message, cause);

		this.status = status;
	}

	public AppException(ApiStatusCode statusCode, Errors validationErrors)
	{
		this(statusCode);

		this.validationErrors = validationErrors;
	}

	public AppException withData(String name, Object value)
	{
		return withData(Map.of(name, value));
	}

	public AppException withData(Map<String, Object> data)
	{
		initDataMap().putAll(data);

		return this;
	}

	private Map<String, Object> initDataMap()
	{
		if (data == null)
		{
			data = new HashMap<>();
		}

		return data;
	}
}
