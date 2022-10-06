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

package com.proit.app.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.util.Version;

import java.util.Comparator;
import java.util.Map;

import static com.proit.app.configuration.ddl.DictsDDLProvider.MIGRATIONS_PATH;
import static com.proit.app.configuration.ddl.DictsDDLProvider.SEPARATOR;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MigrationUtils
{
	public static String getFileHash(String script)
	{
		return DigestUtils.md5Hex(script);
	}

	public static String parseVersion(String migrationName)
	{
		var version = migrationName.replaceAll(".*V(([0-9]+_)+).*", "$1")
				.replace("_", ".");

		return version.substring(0, version.length() - 1);
	}

	public static Comparator<Map.Entry<String, String>> versionComparator()
	{
		return Comparator.comparing(it -> Version.parse(parseVersion(MIGRATIONS_PATH + SEPARATOR + it.getKey())));
	}
}
