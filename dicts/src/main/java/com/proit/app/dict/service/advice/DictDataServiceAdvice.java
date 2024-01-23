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

package com.proit.app.dict.service.advice;

import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.domain.DictItem;
import com.proit.app.dict.model.dictitem.DictDataItem;
import org.springframework.data.domain.Pageable;

import java.util.List;

// TODO: advice надо регистрировать на конкретный dict, чтобы исключить лишние бесполезные вызовы,
// TODO: соответственно из интерфейса этот параметр уйдёт.
public interface DictDataServiceAdvice
{
	default void handleGetByIds(Dict dict, List<String> ids)
	{
	}

	default void handleGetByFilter(Dict dict, List<String> selectFields, String query, Pageable pageable)
	{
	}

	default void handleExistsById(Dict dict, String itemId)
	{
	}

	default void handleExistsByFilter(Dict dict, String query)
	{
	}

	default void handleCountByFilter(Dict dict, String query)
	{
	}

	default void handleBeforeCreate(DictDataItem item)
	{
	}

	default void handleAfterCreate(Dict dict, DictItem item)
	{
	}

	default void handleBeforeCreateMany(List<DictDataItem> items)
	{
		items.forEach(this::handleBeforeCreate);
	}

	default void handleAfterCreateMany(Dict dict, List<DictItem> items)
	{
		items.forEach(item -> handleAfterCreate(dict, item));
	}

	default void handleUpdate(Dict dict, DictItem oldItem, DictDataItem dictDataItem)
	{
	}

	default void handleAfterUpdate(Dict dict, DictItem item)
	{
	}

	default void handleDelete(Dict dict, DictItem item, boolean deleted)
	{
	}

	default void handleDeleteAll(Dict dict, boolean deleted)
	{
	}
}
