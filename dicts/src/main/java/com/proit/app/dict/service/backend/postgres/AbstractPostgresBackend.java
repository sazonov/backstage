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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.exception.TriFunctionException;
import com.proit.app.dict.service.migration.DictTransactionProvider;
import com.proit.app.dict.exception.BiFunctionException;
import com.proit.app.dict.model.postgres.backend.PostgresWord;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractPostgresBackend
{
	@Autowired
	protected ObjectMapper mapper;

	@Autowired
	@Qualifier("dictJdbcTemplate")
	protected NamedParameterJdbcTemplate jdbc;

	@Autowired
	@Qualifier("dictTransactionTemplate")
	protected TransactionTemplate transactionTemplate;

	@Autowired
	protected PostgresEngine postgresEngine;

	@Autowired
	protected DictTransactionProvider dictTransactionProvider;

	@Autowired
	private PostgresReservedKeyword reservedKeywords;

	protected void addTransactionData(Dict dict, boolean schemeUsed)
	{
		dictTransactionProvider.addDictTransactionItem(dict, schemeUsed);
	}

	protected <F, E extends Throwable, T extends Throwable> void transactionWithoutResult(Runnable runnable, F dictId, BiFunctionException<F, E, T> exception)
	{
		try
		{
			run(runnable);
		}
		catch (Exception e)
		{
			throw new RuntimeException(exception.apply(dictId, (E) e));
		}
	}

	protected <F, S, E extends Throwable, T extends Throwable> void transactionWithoutResult(Runnable runnable, F dictId, S dictElementId, TriFunctionException<F, S, E, T> exception)
	{
		try
		{
			run(runnable);
		}
		catch (Exception e)
		{
			throw new RuntimeException(exception.apply(dictId, dictElementId, (E) e));
		}
	}

	protected <R, F, E extends Throwable, T extends Throwable> R transactionWithResult(Callable<R> callable, F dictId, BiFunctionException<F, E, T> exception)
	{
		try
		{
			return result(callable);
		}
		catch (Exception e)
		{
			throw new RuntimeException(exception.apply(dictId, (E) e));
		}
	}

	protected <R, F, S, E extends Throwable, T extends Throwable> R transactionWithResult(Callable<R> callable, F dictId, S dictElementId, TriFunctionException<F, S, E, T> exception)
	{
		try
		{
			return result(callable);
		}
		catch (Exception e)
		{
			throw new RuntimeException(exception.apply(dictId, dictElementId, (E) e));
		}
	}

	protected Map<String, PostgresWord> wordMap(String... words)
	{
		return reservedKeywords.postgresWordMap(words);
	}

	protected Map<String, PostgresWord> wordMap(List<String> wordList, String... words)
	{
		return Stream.concat(wordList.stream(), Arrays.stream(words))
				.map(reservedKeywords::postgresWordMap)
				.map(Map::entrySet)
				.flatMap(Collection::stream)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldV, newV) -> oldV));
	}

	protected String sqlWithParameters(String sql, Map<String, String> parameterMap)
	{
		return StrSubstitutor.replace(sql, parameterMap);
	}

	protected void addParameter(MapSqlParameterSource parameterMap, String parameterName, Object value)
	{
		parameterMap.addValue(parameterName, value);
	}

	private <R> R result(Callable<R> callable)
	{
		return transactionTemplate.execute(status -> {
			try
			{
				return callable.call();
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		});
	}

	private void run(Runnable runnable)
	{
		transactionTemplate.executeWithoutResult(status -> runnable.run());
	}
}
