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

package com.proit.app.controller;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.proit.app.configuration.properties.ApiProperties;
import com.proit.app.exception.AppException;
import com.proit.app.model.api.ApiResponse;
import com.proit.app.model.api.ApiStatusCode;
import com.proit.app.model.api.ApiStatusCodeImpl;
import com.proit.app.utils.ExceptionHandlerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import java.util.Map;
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
	public Object handleException(HttpServletRequest request, HttpServletResponse response, Exception ex)
	{
		log.error("Handling global mvc exception.", ex);

		var appException = extract(ex, AppException.class);

		if (appException != null)
		{
			return handleAppException(request, response, appException);
		}
		else
		{
			return getResponseObject(request, response, createResponse(ApiStatusCodeImpl.UNKNOWN_ERROR, ex));
		}
	}

	@ExceptionHandler(AppException.class)
	public Object handleAppException(HttpServletRequest request, HttpServletResponse response, AppException ex)
	{
		log.error("Handling global mvc exception.", ex);

		return getResponseObject(request, response, processAppException(ex));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public Object handleConstraintViolationException(HttpServletRequest request, HttpServletResponse response,
	                                                 ConstraintViolationException ex)
	{
		log.error("Handling request validation exception.", ex);

		var resp = ApiResponse.of(ApiStatusCodeImpl.ILLEGAL_INPUT);
		resp.setValidationErrors(ExceptionHandlerUtils.buildValidationErrors(ex));

		return getResponseObject(request, response, resp);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public Object handleMethodArgumentNotValid(HttpServletRequest request, HttpServletResponse response,
	                                           MethodArgumentNotValidException ex)
	{
		log.error("Handling request validation exception.", ex);

		var resp = ApiResponse.of(ApiStatusCodeImpl.ILLEGAL_INPUT);
		resp.setValidationErrors(ExceptionHandlerUtils.buildValidationErrors(ex));

		return getResponseObject(request, response, resp);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public Object handleMissingRequestArgument(HttpServletRequest request, HttpServletResponse response, MissingServletRequestParameterException ex)
	{
		log.error("Handling missing request argument exception.", ex);

		return getResponseObject(request, response, ApiResponse.of(ApiStatusCodeImpl.ILLEGAL_INPUT, String.format("Отсутствует обязательный атрибут: %s.", ex.getParameterName())));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public Object handleMethodArgumentTypeMismatch(HttpServletRequest request, HttpServletResponse response, MethodArgumentTypeMismatchException ex)
	{
		log.error("Handling argument type mismatch exception.", ex);

		var type = ex.getRequiredType();

		if (type == null)
		{
			return getResponseObject(request, response, ApiResponse.of(ApiStatusCodeImpl.ILLEGAL_DATA_FORMAT));
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

		return getResponseObject(request, response, ApiResponse.of(ApiStatusCodeImpl.ILLEGAL_DATA_FORMAT, message));
	}


	@ExceptionHandler(HttpMessageNotReadableException.class)
	public Object handleHttpMessageNotReadable(HttpServletRequest request, HttpServletResponse response, HttpMessageNotReadableException ex)
	{
		log.error("Handling message not readable exception.", ex);

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

			return getResponseObject(request, response, ApiResponse.of(ApiStatusCodeImpl.ILLEGAL_DATA_FORMAT, message));
		}
		else
		{
			return getResponseObject(request, response, ApiResponse.of(ApiStatusCodeImpl.ILLEGAL_DATA_FORMAT));
		}
	}

	@ExceptionHandler(AccessDeniedException.class)
	public Object handleHttpAccessDenied(HttpServletRequest request, HttpServletResponse response)
	{
		log.error("Handling access denied exception.");

		return getResponseObject(request, response, ApiResponse.of(ApiStatusCodeImpl.ACCESS_RIGHTS_ERROR));
	}

	private ApiResponse<Object> processAppException(AppException ex)
	{
		var resp = createResponse(ex.getStatus(), ex);

		Optional.ofNullable(ex.getMessage()).ifPresent(resp::setMessage);
		Optional.ofNullable(ex.getValidationErrors()).ifPresent(ExceptionHandlerUtils::buildValidationErrors);
		Optional.ofNullable(ex.getData()).ifPresent(resp::setData);

		return resp;
	}

	private ApiResponse<Object> createResponse(ApiStatusCode statusCode, Exception e)
	{
		var resp = ApiResponse.of(statusCode);

		if (statusCode == ApiStatusCodeImpl.UNKNOWN_ERROR)
		{
			resp.setExceptionCode(RandomStringUtils.randomAlphanumeric(6));

			if (apiProperties.isStackTraceOnError())
			{
				resp.setStackTrace(ExceptionUtils.getRootCauseStackTrace(e));
			}
		}

		return resp;
	}

	private <T> Object getResponseObject(HttpServletRequest request, HttpServletResponse response, ApiResponse<T> resp)
	{
		if (request.getHeader(HttpHeaders.ACCEPT) != null && request.getHeader(HttpHeaders.ACCEPT).contains(MediaType.TEXT_HTML_VALUE))
		{
			switch (resp.getCategory())
			{
				case SUCCESS -> response.setStatus(200);
				case NOT_FOUND -> response.setStatus(404);
				case ACCESS_RIGHTS -> response.setStatus(403);
				default -> response.setStatus(500);
			}

			return new ModelAndView("exception/error", Map.of("response", resp));
		}
		else
		{
			return ResponseEntity.ok(resp);
		}
	}
}
