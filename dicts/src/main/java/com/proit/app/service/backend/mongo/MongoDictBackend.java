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

package com.proit.app.service.backend.mongo;

import com.proit.app.domain.Dict;
import com.proit.app.domain.DictEnum;
import com.proit.app.exception.dict.DictDeletedException;
import com.proit.app.exception.dict.DictNotFoundException;
import com.proit.app.exception.dict.enums.EnumNotFoundException;
import com.proit.app.service.backend.DictBackend;
import com.proit.app.service.backend.Engine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MongoDictBackend extends AbstractMongoBackend implements DictBackend
{
	@Override
	public Engine getEngine()
	{
		return mongoEngine;
	}

	@Override
	public Dict getDictById(String id)
	{
		Assert.notNull(id, "dictId не может быть null.");

		var dict = mongoDictRepository.findById(id)
				.orElseThrow(() -> new DictNotFoundException(id));

		if (dict.getDeleted() != null)
		{
			throw new DictDeletedException(id);
		}

		convertMongoServiceFields(dict);

		return dict;
	}

	@Override
	public List<Dict> getAllDicts()
	{
		var result = mongoDictRepository.findAll();

		result.forEach(this::convertMongoServiceFields);

		return result;
	}

	@Override
	public Dict saveDict(Dict dict)
	{
		return save(dict);
	}

	@Override
	public Dict updateDict(String id, Dict dict)
	{
		return save(dict);
	}

	@Override
	public void deleteById(String id)
	{
		mongoDictRepository.deleteById(id);
	}

	@Override
	//	TODO: История изменений схемы, даты создания/обновления схемы?
	public void softDeleteById(String id, LocalDateTime deleted)
	{
		var dict = mongoDictRepository.findById(id)
				.orElseThrow(() -> new DictNotFoundException(id));

		addTransactionData(id, true);

		dict.setDeleted(deleted);

		save(dict);
	}

	@Override
	public boolean existsById(String id)
	{
		return mongoDictRepository.existsById(id);
	}

	@Override
	public DictEnum createEnum(Dict dict, DictEnum dictEnum)
	{
		addTransactionData(null, true);

		save(dict);

		return dictEnum;
	}

	@Override
	public DictEnum updateEnum(Dict dict, DictEnum dictEnum)
	{
		addTransactionData(null, true);

		var oldEnum = dict.getEnums()
				.stream()
				.filter(it -> it.getId().equals(dictEnum.getId()))
				.findAny()
				.orElseThrow(() -> new EnumNotFoundException(dictEnum.getId()));

		oldEnum.setValues(dictEnum.getValues());
		oldEnum.setName(dictEnum.getName());

		save(dict);

		return dictEnum;
	}

	@Override
	public void deleteEnum(Dict dict, String enumId)
	{
		addTransactionData(null, true);

		save(dict);
	}

	private Dict save(Dict dict)
	{
		return mongoDictRepository.save(dict);
	}
}
