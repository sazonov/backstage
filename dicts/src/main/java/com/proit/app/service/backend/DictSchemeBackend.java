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

import com.proit.app.domain.Dict;
import com.proit.app.domain.DictConstraint;
import com.proit.app.domain.DictField;
import com.proit.app.domain.DictIndex;

public interface DictSchemeBackend extends Backend
{
	Dict createDictScheme(Dict dict);

	Dict updateDictScheme(String dictId, Dict updatedDict);

	void renameDictSchemeById(String dictId, String renamedDictId);

	void deleteDictSchemeById(String dictId);

	boolean existsDictSchemeById(String dictId);

	DictField renameDictField(Dict dict, String oldFieldId, DictField field);

	DictConstraint createConstraint(Dict dict, DictConstraint constraint);

	void deleteConstraint(Dict dict, String id);

	DictIndex createIndex(Dict dict, DictIndex index);

	void deleteIndex(Dict dict, String id);
}
