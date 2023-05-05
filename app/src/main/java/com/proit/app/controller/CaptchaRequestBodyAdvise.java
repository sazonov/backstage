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

import com.proit.app.configuration.properties.CaptchaProperties;
import com.proit.app.exception.AppException;
import com.proit.app.model.api.ApiStatusCodeImpl;
import com.proit.app.model.dto.captcha.CaptchaTokenRequest;
import com.proit.app.service.captcha.CaptchaService;
import com.proit.app.service.captcha.CaptchaServiceAdvise;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
@ConditionalOnProperty(CaptchaProperties.ACTIVATION_PROPERTY)
public class CaptchaRequestBodyAdvise implements RequestBodyAdvice
{
	@Autowired(required = false)
	private List<CaptchaServiceAdvise> advises = new ArrayList<>();

	@Autowired
	private CaptchaService captchaService;

	@Override
	public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType)
	{
		return CaptchaTokenRequest.class.isAssignableFrom((Class) targetType);
	}

	@Override
	public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException
	{
		return inputMessage;
	}

	@Override
	public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType)
	{
		if (advises.stream().allMatch(it -> it.isCaptchaEnabled((CaptchaTokenRequest) body)))
		{
			var captchaTokenRequest = (CaptchaTokenRequest) body;

			if (!captchaService.check(captchaTokenRequest.getToken()))
			{
				throw new AppException(ApiStatusCodeImpl.CAPTCHA_CHECK_ERROR);
			}
		}

		return body;
	}

	@Override
	public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType)
	{
		return body;
	}
}
