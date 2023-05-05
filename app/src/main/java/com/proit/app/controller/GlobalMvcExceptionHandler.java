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

package com.proit.app.controller;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.proit.app.configuration.properties.ApiProperties;
import com.proit.app.exception.AppException;
import com.proit.app.model.api.ApiResponse;
import com.proit.app.model.api.ApiStatusCodeImpl;
import com.proit.app.utils.ExceptionHandlerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolationException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.proit.app.utils.ExceptionUtils.extract;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalMvcExceptionHandler
{
	private final ApiProperties apiProperties;

	@ExceptionHandler(Exception.class)
	public Object handleException(Exception ex)
	{
		ApiResponse<Object> apiResponse;
		var appException = extract(ex, AppException.class);

		if (appException != null)
		{
			apiResponse = ApiResponse.of(appException.getStatus());

			Optional.ofNullable(appException.getMessage()).ifPresent(apiResponse::setMessage);
			Optional.ofNullable(appException.getValidationErrors()).ifPresent(ExceptionHandlerUtils::buildValidationErrors);
			Optional.ofNullable(appException.getData()).ifPresent(apiResponse::setData);
		}
		else
		{
			apiResponse = ApiResponse.of(ApiStatusCodeImpl.UNKNOWN_ERROR);

			if (apiProperties.isStackTraceOnError())
			{
				apiResponse.setStackTrace(ExceptionUtils.getRootCauseStackTrace(ex));
			}
		}

		apiResponse.setExceptionCode(RandomStringUtils.randomAlphanumeric(6));

		log.error("Handling global mvc exception. Exception code: {}.", apiResponse.getExceptionCode(), ex);

		return ResponseEntity.ok(apiResponse);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public Object handleConstraintViolationException(ConstraintViolationException ex)
	{
		log.error("Handling request validation exception.", ex);

		var apiResponse = ApiResponse.of(ApiStatusCodeImpl.ILLEGAL_INPUT);
		apiResponse.setValidationErrors(ExceptionHandlerUtils.buildValidationErrors(ex));

		return ResponseEntity.ok(apiResponse);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public Object handleMethodArgumentNotValid(MethodArgumentNotValidException ex)
	{
		log.error("Handling request validation exception.", ex);

		var apiResponse = ApiResponse.of(ApiStatusCodeImpl.ILLEGAL_INPUT);
		apiResponse.setValidationErrors(ExceptionHandlerUtils.buildValidationErrors(ex));

		return ResponseEntity.ok(apiResponse);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public Object handleMissingRequestArgument(MissingServletRequestParameterException ex)
	{
		log.error("Handling missing request argument exception.", ex);

		return ResponseEntity.ok(ApiResponse.of(ApiStatusCodeImpl.ILLEGAL_INPUT, String.format("Отсутствует обязательный атрибут: %s.", ex.getParameterName())));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public Object handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex)
	{
		log.error("Handling argument type mismatch exception.", ex);

		var type = ex.getRequiredType();

		if (type == null)
		{
			return ApiResponse.of(ApiStatusCodeImpl.ILLEGAL_DATA_FORMAT);
		}

		var method = ex.getParameter().getMethod();
		var message = "Параметр " + ex.getName() + (method != null ? (" метода " + method.getName() + " ") : " ");

		if (type.isEnum())
		{
			message += "может принимать значения: " + StringUtils.join(type.getEnumConstants(), ", ") + ".";
		}
		else
		{
			message += "должен быть типа " + type.getSimpleName() + ".";
		}

		return ResponseEntity.ok(ApiResponse.of(ApiStatusCodeImpl.ILLEGAL_DATA_FORMAT, message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public Object handleHttpMessageNotReadable(HttpMessageNotReadableException ex)
	{
		log.error("Handling message not readable exception.", ex);

		ApiResponse<Object> apiResponse;

		if (ex.getCause() instanceof InvalidFormatException cause)
		{
			var path = cause.getPath()
					.stream()
					.map(JsonMappingException.Reference::getFieldName)
					.filter(Objects::nonNull)
					.collect(Collectors.joining("."));

			var message = "Параметр " + path + " ";
			var type = cause.getTargetType();

			if (type.isEnum())
			{
				message += "может принимать значения: " + StringUtils.join(type.getEnumConstants(), ", ") + ".";
			}
			else
			{
				message += "должен быть типа " + type.getSimpleName() + ".";
			}

			apiResponse = ApiResponse.of(ApiStatusCodeImpl.ILLEGAL_DATA_FORMAT, message);
		}
		else
		{
			apiResponse = ApiResponse.of(ApiStatusCodeImpl.ILLEGAL_DATA_FORMAT);
		}

		return ResponseEntity.ok(apiResponse);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public Object handleHttpAccessDenied()
	{
		log.error("Handling access denied exception.");

		return ResponseEntity.ok(ApiResponse.of(ApiStatusCodeImpl.ACCESS_RIGHTS_ERROR));
	}
}
