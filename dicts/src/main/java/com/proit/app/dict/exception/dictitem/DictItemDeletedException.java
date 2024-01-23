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

package com.proit.app.dict.exception.dictitem;

import com.proit.app.dict.exception.dict.DictException;

public class DictItemDeletedException extends DictException
{
	public DictItemDeletedException(String dictId, String dictItemId, Throwable throwable)
	{
		super("При удалении dictItem '%s' для справочника '%s' произошла ошибка.".formatted(dictItemId, dictId), throwable);
	}
}