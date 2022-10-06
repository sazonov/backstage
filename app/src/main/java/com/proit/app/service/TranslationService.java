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

package com.proit.app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TranslationService
{
	private final MessageSource messageSource;

	public String translate(String code, Object ... args)
	{
		try
		{
			return messageSource.getMessage(code, args, new Locale("ru"));
		}
		catch (Exception e)
		{
			return String.format("%s_%s", code, getCurrentLocale());
		}
	}

	public String getCurrentLocale()
	{
		return LocaleContextHolder.getLocale().getLanguage();
	}
}
