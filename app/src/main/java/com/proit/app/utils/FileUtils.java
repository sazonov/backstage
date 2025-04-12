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

package com.proit.app.utils;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileUtils
{
	public static Path writeResourceToTempFile(String resourceName)
	{
		return writeStreamToTempFile(FileUtils.class.getResourceAsStream(resourceName));
	}

	public static Path writeUnicodeStringToTempFile(String string)
	{
		return writeStreamToTempFile(IOUtils.toInputStream(string, StandardCharsets.UTF_8));
	}

	public static Path writeStringToTempFile(String string, Charset charset)
	{
		return writeStreamToTempFile(IOUtils.toInputStream(string, charset));
	}

	public static Path writeStreamToTempFile(InputStream stream)
	{
		try
		{
			Path target = Files.createTempFile("temp_", "_temp");

			org.apache.commons.io.FileUtils.copyInputStreamToFile(stream, target.toFile());

			return target;
		}
		catch (IOException e)
		{
			throw new RuntimeException("Failed to write stream to temp file", e);
		}
	}

	public static List<String> extractLines(String path)
	{
		try
		{
			var inputStream = new PathMatchingResourcePatternResolver()
					.getResource(path)
					.getInputStream();

			return IOUtils.readLines(inputStream, StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			throw new RuntimeException("Failed extract lines from '%s': %s");
		}
	}
}
