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

package com.proit.app.api.service;

import com.proit.app.api.configuration.properties.CaptchaProperties;
import com.proit.app.api.model.integration.CaptchaResponse;
import com.proit.app.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(CaptchaProperties.ACTIVATION_PROPERTY)
@EnableConfigurationProperties(CaptchaProperties.class)
public class CaptchaService
{
	private final CaptchaProperties captchaProperties;

	public boolean check(String token)
	{
		var url = UriComponentsBuilder.fromHttpUrl(captchaProperties.getApi())
				.queryParam("secret", captchaProperties.getSecret())
				.queryParam("response", token).build().toString();

		var response = new RestTemplate().getForObject(url, CaptchaResponse.class);

		boolean success = response != null && response.isSuccess();
		String score = success ? String.valueOf(response.getScore()) : "-";

		log.info("Captcha checked for user '{}'. Success: {}, score: {}.", SecurityUtils.getCurrentUserId(), success, score);

		return success;
	}
}
