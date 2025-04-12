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

package com.proit.app.dict.service.backend;

import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.domain.DictFieldName;
import com.proit.app.dict.domain.DictItem;
import com.proit.app.dict.service.query.ast.QueryExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DictDataBackend extends Backend
{
	DictItem getById(Dict dict, String id, List<DictFieldName> requiredFields);

	List<DictItem> getByIds(Dict dict, List<String> ids, List<DictFieldName> requiredFields);

	Page<DictItem> getByFilter(Dict dict, List<DictFieldName> requiredFields, QueryExpression queryExpression, Pageable pageable);

	boolean existsById(Dict dict, String itemId);

	boolean existsByFilter(Dict dict, QueryExpression queryExpression);

	DictItem create(Dict dict, DictItem dictItem);

	List<DictItem> createMany(Dict dict, List<DictItem> dictItems);

	DictItem update(Dict dict, String itemId, DictItem dictItem, long version);

	void delete(Dict dict, DictItem dictItem);

	void deleteAll(Dict dict, List<DictItem> dictItems);

	long countByFilter(Dict dict, QueryExpression queryExpression);
}
