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

package com.proit.app.service.backend;

import com.proit.app.domain.DictFieldName;
import com.proit.app.domain.DictItem;
import com.proit.app.service.query.ast.QueryExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DictDataBackend
{
	Engine getEngine();

	DictItem getById(String dictId, String id, List<DictFieldName> requiredFields);

	List<DictItem> getByIds(String dictId, List<String> ids, List<DictFieldName> requiredFields);

	Page<DictItem> getByFilter(String dictId, List<DictFieldName> requiredFields, QueryExpression queryExpression, Pageable pageable);

	boolean existsById(String dictId, String itemId);

	boolean existsByFilter(String dictId, QueryExpression queryExpression);

	DictItem create(String dictId, DictItem dictItem);

	List<DictItem> createMany(String dictId, List<DictItem> dictItems);

	DictItem update(String dictId, String itemId, long version, DictItem dictItem);

	void delete(String dictId, String itemId, boolean deleted, String reason);

	void deleteAll(String dictId, boolean deleted);

	long countByFilter(String dictId, QueryExpression queryExpression);
}
