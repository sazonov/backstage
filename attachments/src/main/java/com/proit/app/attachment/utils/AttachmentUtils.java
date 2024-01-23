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

package com.proit.app.attachment.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AttachmentUtils
{
	private static final Map<String, String> ext2mime;

	static
	{
		HashMap<String, String> e2m = new HashMap<>();
		e2m.put("jpg", "image/jpeg");
		e2m.put("txt", "text/plain");
		e2m.put("png", "image/png");
		e2m.put("pdf", "application/pdf");
		e2m.put("bmp", "image/bmp");
		e2m.put("doc", "application/msword");
		e2m.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		e2m.put("xls", "application/vnd.ms-excel");
		e2m.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		e2m.put("csv", "text/csv");
		e2m.put("json", "application/json");

		ext2mime = Map.copyOf(e2m);
	}

	public static String extensionToMimeType(String mimeType)
	{
		return ext2mime.get(mimeType.toLowerCase());
	}

	public static String buildContentDisposition(boolean inline, String legacyName)
	{
		return buildContentDisposition(inline, legacyName, legacyName);
	}

	public static String buildContentDisposition(boolean inline, String legacyName, String unicodeName)
	{
		try
		{
			return inline ? "inline" : "attachment" + "; filename=\"" + legacyName + "\"; filename*=utf-8''" + URLEncoder.encode(unicodeName, StandardCharsets.UTF_8).replace("+", "%20");
		}
		catch (Exception e)
		{
			throw new RuntimeException("failed to form content disposition header", e);
		}
	}
}
