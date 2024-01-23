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

import com.proit.app.dict.model.postgres.backend.PostgresOrder;
import com.proit.app.dict.model.postgres.backend.PostgresPageable;
import com.proit.app.dict.model.postgres.backend.PostgresSort;
import com.proit.app.dict.service.mapping.DictFieldNameMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostgresPageableMapper
{
	private final PostgresDictFieldNameMapper postgresFieldNameMapper;

	private final DictFieldNameMappingService fieldNameMappingService;

	public PostgresPageable mapFrom(String dictId, Pageable pageable)
	{
		if (pageable.isPaged())
		{
			var postgresSort = postgresSort(dictId, pageable.getSort());

			return PostgresPageable.of(pageable.isPaged(), pageable.getPageNumber(), pageable.getPageSize(), pageable.getOffset(), postgresSort);
		}
		else
		{
			return PostgresPageable.UNPAGED;
		}
	}

	private PostgresSort postgresSort(String dictId, Sort sort)
	{
		var orderMap = sort.stream()
				.collect(Collectors.toMap(Sort.Order::getProperty, Function.identity()));

		var fieldNameMap = orderMap.entrySet()
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey, it -> postgresFieldNameMapper.mapFrom(dictId, fieldNameMappingService.mapDictFieldName(it.getKey()))));

		var postgresOrders = fieldNameMap.entrySet()
				.stream()
				.map(it -> new PostgresOrder(it.getValue(), orderMap.get(it.getKey()).getDirection()))
				.toList();

		return new PostgresSort(postgresOrders);
	}
}
