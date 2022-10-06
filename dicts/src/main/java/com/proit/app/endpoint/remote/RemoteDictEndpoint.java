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

package com.proit.app.endpoint.remote;

import com.proit.app.conversion.dto.DictConverter;
import com.proit.app.conversion.dto.DictEnumConverter;
import com.proit.app.conversion.dto.DictEnumRequestConverter;
import com.proit.app.conversion.dto.DictRequestConverter;
import com.proit.app.domain.DictEnum;
import com.proit.app.domain.DictFieldType;
import com.proit.app.model.api.ApiResponse;
import com.proit.app.model.dto.DictDto;
import com.proit.app.model.dto.DictEnumDto;
import com.proit.app.model.dto.request.CreateDictEnumRequest;
import com.proit.app.model.dto.request.CreateDictRequest;
import com.proit.app.model.dto.request.DeleteDictRequest;
import com.proit.app.service.DictService;
import com.proit.app.service.remote.RemoteDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/remote/dicts")
@Tag(name = "remote-dict-endpoint", description = "Методы для работы со схемами справочников (Remote).")
public class RemoteDictEndpoint implements RemoteDictService
{
	private final DictConverter schemeConverter;
	private final DictEnumConverter dictEnumConverter;
	private final DictRequestConverter schemeRequestConverter;
	private final DictEnumRequestConverter dictEnumRequestConverter;

	private final DictService schemeService;

	@Override
	@Operation(summary = "Получение схемы справочника по идентификатору.")
	@GetMapping("/get")
	public ApiResponse<DictDto> get(@RequestParam String id)
	{
		return ApiResponse.of(schemeConverter.convert(schemeService.getById(id)));
	}

	@Override
	@Operation(summary = "Получение схем всех существующих справочников.")
	@GetMapping("/list")
	public ApiResponse<List<DictDto>> list()
	{
		return ApiResponse.of(schemeConverter.convert(schemeService.getAll()));
	}

	@Override
	@Operation(summary = "Добавление справочника.")
	@PostMapping("/create")
	public ApiResponse<DictDto> create(@RequestBody @Valid CreateDictRequest request)
	{
		//fixme реализация для KGH-2695, зарефакторить в рамках KGH-2728
		var dict = schemeRequestConverter.convert(request);

		var sourceFields = new ArrayList<>(dict.getFields());

		var fields = dict.getFields().stream()
				.filter(not(field -> field.getType() == DictFieldType.ENUM))
				.collect(Collectors.toList());
		dict.setFields(fields);

		var createdDict = schemeService.create(dict);

		dict.getEnums().forEach(dictEnum -> schemeService.createEnum(createdDict.getId(), dictEnum));

		dict.setFields(sourceFields);

		var result = schemeConverter.convert(schemeService.update(dict.getId(), dict));

		log.info("Справочник {} был успешно создан.", result.getId());

		return ApiResponse.of(result);
	}

	@Override
	@Operation(summary = "Добавление enum в справочник.")
	@PostMapping("/enum/create")
	public ApiResponse<DictEnumDto> createEnum(@RequestBody @Valid CreateDictEnumRequest request, @RequestParam String dictId)
	{
		var dictEnum = dictEnumRequestConverter.convert(request);
		var result = dictEnumConverter.convert(schemeService.createEnum(dictId, dictEnum));

		log.info("Для справочника {} был успешно создан enum {}.", dictId, result.getId());

		return ApiResponse.of(result);
	}

	@Override
	@Operation(summary = "Обновление enum в справочнике.")
	@PostMapping("/enum/update")
	public ApiResponse<DictEnumDto> updateEnum(@RequestBody @Valid CreateDictEnumRequest request, @RequestParam String dictId)
	{
		var dictEnum = dictEnumRequestConverter.convert(request);
		var result = dictEnumConverter.convert(schemeService.updateEnum(dictId, dictEnum));

		log.info("Для справочника {} был успешно обновлен enum {}.", dictId, result.getId());

		return ApiResponse.of(result);
	}

	@Override
	@Operation(summary = "Удаление enum в справочнике.")
	@PostMapping("/enum/delete")
	public ApiResponse<?> deleteEnum(@RequestParam String enumId, @RequestParam String dictId)
	{
		schemeService.deleteEnum(dictId, enumId);

		log.info("Для справочника {} был удален enum {}.", dictId, enumId);

		return ApiResponse.ok();
	}

	@Override
	@Operation(summary = "Обновление справочника.")
	@PostMapping("/update")
	public ApiResponse<DictDto> update(@RequestBody @Valid CreateDictRequest request)
	{
		//		TODO: dictId = отдельным параметром
		//fixme реализация для KGH-2695, зарефакторить в рамках KGH-2728
		var dict = schemeRequestConverter.convert(request);

		var sourceFields = new ArrayList<>(dict.getFields());

		var oldDict = schemeService.getById(dict.getId());

		var oldDictEnumIds = oldDict.getEnums()
				.stream()
				.map(DictEnum::getId)
				.toList();

		var actualEnumIds = dict.getEnums()
				.stream()
				.map(DictEnum::getId)
				.toList();

		var enumsForUpdate = dict.getEnums()
				.stream()
				.filter(dictEnum -> oldDictEnumIds.contains(dictEnum.getId()))
				.toList();

		var enumsForUpdateIds = enumsForUpdate.stream()
				.map(DictEnum::getId)
				.toList();

		var enumsForSave = dict.getEnums().stream()
				.filter(not(dictEnum -> oldDictEnumIds.contains(dictEnum.getId()))).toList();

		var enumsForDelete = oldDict.getEnums().stream()
				.filter(not(oldDictEnum -> actualEnumIds.contains(oldDictEnum.getId()))).toList();

		var fields = sourceFields.stream()
				.filter(dictField -> dictField.getType() != DictFieldType.ENUM || enumsForUpdateIds.contains(dictField.getEnumId()))
				.collect(Collectors.toList());

		dict.setFields(fields);

		schemeService.update(dict.getId(), dict);

		enumsForUpdate.forEach(dictEnum -> schemeService.updateEnum(dict.getId(), dictEnum));

		enumsForDelete.forEach(dictEnum -> schemeService.deleteEnum(dict.getId(), dictEnum.getId()));

		enumsForSave.forEach(dictEnum -> schemeService.createEnum(dict.getId(), dictEnum));

		dict.setFields(sourceFields);

		var result = schemeConverter.convert(schemeService.update(dict.getId(), dict));

		log.info("Справочник {} был успешно обновлен.", result.getId());

		return ApiResponse.of(result);
	}

	@Override
	@Operation(summary = "Удаление справочника (soft delete).")
	@PostMapping("/delete")
	public ApiResponse<?> delete(@RequestBody @Valid DeleteDictRequest request)
	{
		schemeService.delete(request.getId(), request.isDeleted());

		log.info("Справочник {} был удален.", request.getId());

		return ApiResponse.ok();
	}
}
