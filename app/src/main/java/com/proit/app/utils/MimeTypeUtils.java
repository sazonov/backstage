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

package com.proit.app.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class MimeTypeUtils
{
	public static String getMimeTypeByFilename(String filename)
	{
		String result = null;

		try
		{
			result = Files.probeContentType(Path.of(filename));
		}
		catch (Exception e)
		{
			log.warn("Failed to detect mime type by filename '%s'".formatted(filename), e);
		}

		if (result == null)
		{
			return MediaType.ALL_VALUE;
		}

		return result;
	}
}
