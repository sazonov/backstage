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

package com.proit.app.dict.service.backend;

import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.domain.DictEnum;

import java.time.LocalDateTime;
import java.util.List;

public interface DictBackend extends Backend
{
	Dict getDictById(String id);

	List<Dict> getAllDicts();

	Dict saveDict(Dict dict);

	Dict updateDict(Dict dict);

	void deleteById(String id);

	void softDelete(Dict dict, LocalDateTime deleted);

	boolean existsById(String id);

	DictEnum createEnum(Dict dict, DictEnum dictEnum);

	// TODO: зачем старый? есть ведь айди, делаем по аналогии с updateDictScheme.
	DictEnum updateEnum(Dict dict, DictEnum dictEnum);

	void deleteEnum(Dict dict, String enumId);
}
