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

package com.proit.app.service.backend;

import com.proit.app.domain.*;

import java.time.LocalDateTime;
import java.util.List;

public interface DictBackend
{
	Storage getStorage();

	Dict getDictById(String id);

	List<Dict> getAllDicts();

	Dict createDict(Dict dict);

	Dict updateDict(String dictId, Dict updatedDict);

	void deleteDict(String id, LocalDateTime deleted);

	DictField renameDictField(Dict dict, String oldFieldId, DictField field);

	DictConstraint createConstraint(Dict dict, DictConstraint constraint);

	void deleteConstraint(Dict dict, String id);

	DictIndex createIndex(Dict dict, DictIndex index);

	void deleteIndex(Dict dict, String id);

	DictEnum createEnum(Dict dict, DictEnum dictEnum);

	// TODO: зачем старый? есть ведь айди, делаем по аналогии с updateDict.
	DictEnum updateEnum(Dict dict, DictEnum oldEnum, DictEnum newEnum);

	void deleteEnum(Dict dict, String enumId);
}
