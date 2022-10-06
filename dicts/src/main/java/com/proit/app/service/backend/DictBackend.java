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

package com.proit.app.service.backend;

import com.proit.app.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface DictBackend
{
	void beginDDL();

	void commitDDL();

	// TODO: зачем тут исключение?
	void rollbackDDL(Exception e);

	// TODO: почему не DictItem возвращается?
	List<Object> getByIds(String dictId, List<String> ids, List<DictFieldName> requiredFields);

	// TODO: почему не DictItem возвращается?
	Page<Object> getByFilter(Dict dict, List<DictFieldName> requiredFields, String filtersQuery, Pageable pageable);

	boolean existsById(String dictId, String itemId);

	boolean existsByFilter(String dictId, String filtersQuery);

	// TODO: почему не DictItem передаётся/возвращается? Доки - это в монго, используем item.
	Map<String, Object> create(String dictId, Map<String, Object> mappedDoc);

	// TODO: почему не DictItem передаётся/возвращается?
	List<Map<String, Object>> createMany(String dictId, List<Map<String, Object>> docs);

	// TODO: почему не DictItem передаётся/возвращается?
	Object update(String dictId, String itemId, long version, Map<String, Object> mappedDoc);

	void delete(String dictId, String itemId, boolean deleted);

	Dict getDictById(String id);

	List<Dict> getAllDicts();

	Dict createDict(Dict dict);

	void deleteDict(String id, LocalDateTime deleted);

	Dict updateDict(String dictId, Dict updatedDict);

	DictField renameDictField(Dict dict, String oldFieldId, DictField field);

	DictConstraint createConstraint(Dict dict, DictConstraint constraint);

	void deleteConstraint(Dict dict, String id);

	DictIndex createIndex(Dict dict, DictIndex index);

	void deleteIndex(Dict dict, String id);

	DictEnum createEnum(Dict dict, DictEnum dictEnum);

	// TODO: зачем старый? есть ведь айди, делаем по аналогии с updateDict.
	DictEnum updateEnum(Dict dict, DictEnum oldEnum, DictEnum newEnum);

	void deleteEnum(Dict dict, String enumId);

	void deleteAll(String dictId, boolean deleted);

	long countByFilter(String dictId, String filtersQuery);
}
