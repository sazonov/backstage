/*
 *
 *  Copyright 2019-2023 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.proit.app.dict.service.backend.postgres;

import com.proit.app.dict.model.postgres.backend.PostgresWord;
import com.proit.app.utils.FileUtils;
import com.proit.app.utils.ValidationUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

//TODO: провести рефакторинг в рамках BACKSTAGE-23
@Component
public class PostgresReservedKeyword
{
	private static final String KEYWORDS_FILE = "db/postgres/reserved_keywords.txt";

	private final Set<String> postgresKeywords = new HashSet<>();

	public PostgresReservedKeyword()
	{
		var keywords = FileUtils.extractLines(KEYWORDS_FILE)
				.stream()
				.map(String::toLowerCase)
				.collect(Collectors.toSet());

		postgresKeywords.addAll(keywords);
	}

	public Map<String, PostgresWord> postgresWordMap(String... words)
	{
		return Arrays.stream(words)
				.collect(Collectors.toMap(
						Function.identity(),
						it -> new PostgresWord(it, keywordCondition(it))
				));
	}

	private boolean keywordCondition(String word)
	{
		return postgresKeywords.contains(word) || ValidationUtils.hasFirstDigit(word);
	}
}
