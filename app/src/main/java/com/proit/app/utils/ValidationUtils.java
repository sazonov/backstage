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

package com.proit.app.utils;

import com.proit.app.service.TranslationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ValidationUtils implements ApplicationContextAware
{
	private static final String FORMATTING_REPLACEMENT_TOKEN = "~";
	private static final String PHONE_NUMBER_PATTERN = "^((\\+7)|(8))\\d{10}$";
	private static final String LATIN_AND_DIGITS_PATTERN = "^[a-zA-Z0-9]*$";
	private static final String LATIN_DIGITS_SYMBOLS_PATTERN = "^[a-zA-Z0-9!@#$%^&*()_\\-+=?/.,<>{}\\[\\]|/~`]*$";

	private static TranslationService translationService;

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
	{
		translationService = applicationContext.getBean(TranslationService.class);
	}

	public static String errorCodeToMessage(String code)
	{
		return translationService.translate(code);
	}

	public static int countLines(String str)
	{
		if (str == null || str.length() == 0)
		{
			return 0;
		}

		int lines = 1;
		int len = str.length();

		for (int pos = 0; pos < len; pos++)
		{
			char c = str.charAt(pos);

			if (c == '\r')
			{
				lines++;

				if (pos + 1 < len && str.charAt(pos + 1) == '\n')
				{
					pos++;
				}
			}
			else if (c == '\n')
			{
				lines++;
			}
		}

		return lines;
	}

	public static boolean isCyrillicOnly(String str)
	{
		for (char ch : str.toCharArray())
		{
			if (!(Character.UnicodeBlock.CYRILLIC.equals(Character.UnicodeBlock.of(ch)) || ch == '-'))
			{
				return false;
			}
		}

		return true;
	}

	public static boolean isCyrillicWithSpacesOnly(String str)
	{
		for (char ch : str.toCharArray())
		{
			if (!(Character.UnicodeBlock.CYRILLIC.equals(Character.UnicodeBlock.of(ch)) || ch == '-' || ch == ' '))
			{
				return false;
			}
		}

		return true;
	}

	public static boolean isCharacterSpecialOrPunctuation(Character ch)
	{
		return !Character.isAlphabetic(ch) && !Character.isDigit(ch) && !Character.isWhitespace(ch);
	}

	public static String removeBBCodes(String source)
	{
		if (source == null)
		{
			return "";
		}

		return source
				.replaceAll("(?s)\\[quote=?(.*?)](.*?)\\[/quote]", FORMATTING_REPLACEMENT_TOKEN)
				.replaceAll("\\[img\\d*].+\\[\\/img\\d*]", FORMATTING_REPLACEMENT_TOKEN)
				.replaceAll("\\[iframe].+\\[\\/iframe]", FORMATTING_REPLACEMENT_TOKEN)
				.replaceAll("\\[/?[A-Za-z0-9\\*]+.*?]", "");
	}

	public static String removeHTML(String source)
	{
		if (source == null)
		{
			return "";
		}

		return source
				.replaceAll("&[A-Za-z]{2,6};", "")
				.replaceAll("<", "&lt;")
				.replaceAll(">", "&gt;");
	}

	public static String removeFormatting(String source)
	{
		return removeFormatting(source, true);
	}

	public static String removeFormatting(String source, boolean keepReplacementTokens)
	{
		String result = removeHTML(removeBBCodes(source));

		if (!keepReplacementTokens)
		{
			result = result.replace(FORMATTING_REPLACEMENT_TOKEN, "");
		}

		return result;
	}

	public static boolean isPhoneNumber(String phoneNumber)
	{
		return StringUtils.isNotBlank(phoneNumber) && phoneNumber.matches(PHONE_NUMBER_PATTERN);
	}

	public static boolean isLatinAndDigitsOnly(String str)
	{
		return StringUtils.isNotBlank(str) && str.matches(LATIN_AND_DIGITS_PATTERN);
	}

	public static boolean isLatinDigitsSymbolsOnly(String str)
	{
		return StringUtils.isNoneBlank(str) && str.matches(LATIN_DIGITS_SYMBOLS_PATTERN);
	}
}
