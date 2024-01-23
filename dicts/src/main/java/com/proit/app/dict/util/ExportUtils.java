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

package com.proit.app.dict.util;

import com.proit.app.dict.api.constant.ExportedDictFormat;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.Date;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExportUtils
{
	public static final String DEFAULT_DATE_PATTERN = "yyyyMMddhhmmss";

	public static String generateFilename(String dictId, ExportedDictFormat format, List<String> itemIds)
	{
		return new StringBuilder()
				.append(dictId)
				.append("_")
				.append(DateFormatUtils.format(new Date(), DEFAULT_DATE_PATTERN))
				.append(itemIds != null && !itemIds.isEmpty() ? "_partial" : "")
				.append(getExtension(format))
				.toString();
	}

	public static String getExtension(ExportedDictFormat format)
	{
		return switch (format)
				{
					case SQL -> ".sql";
					case CSV -> ".csv";
					case JSON -> ".json";
				};
	}
}
