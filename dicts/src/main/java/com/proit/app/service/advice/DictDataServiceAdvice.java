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

package com.proit.app.service.advice;

import com.proit.app.domain.DictItem;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

// TODO: advice надо регистрировать на конкретный dict, чтобы исключить лишние бесполезные вызовы,
// TODO: соответственно из интерфейса этот параметр уйдёт.
public interface DictDataServiceAdvice
{
	default void handleGetByIds(String dictId, List<String> ids)
	{
	}

	default void handleGetByFilter(String dictId, List<String> selectFields, String query, Pageable pageable)
	{
	}

	default void handleExistsById(String dictId, String itemId)
	{
	}

	default void handleExistsByFilter(String dictId, String query)
	{
	}

	default void handleCountByFilter(String dictId, String query)
	{
	}

	default void handleBeforeCreate(String dictId, Map<String, Object> item)
	{
	}

	default void handleAfterCreate(String dictId, DictItem item)
	{
	}

	default void handleBeforeCreateMany(String dictId, List<Map<String, Object>> items)
	{
		items.forEach(item -> handleBeforeCreate(dictId, item));
	}

	default void handleAfterCreateMany(String dictId, List<DictItem> items)
	{
		items.forEach(item -> handleAfterCreate(dictId, item));
	}

	default void handleUpdate(String dictId, DictItem oldItem, Map<String, Object> updatedItem)
	{
	}

	default void handleDelete(String dictId, DictItem item, boolean deleted)
	{
	}

	default void handleDeleteAll(String dictId, boolean deleted)
	{
	}
}
