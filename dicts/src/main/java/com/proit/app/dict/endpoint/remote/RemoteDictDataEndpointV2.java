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

package com.proit.app.dict.endpoint.remote;

import com.proit.app.dict.conversion.dto.DictConverter;
import com.proit.app.dict.conversion.dto.data.DictItemConverter;
import com.proit.app.api.model.ApiResponse;
import com.proit.app.api.model.OkResponse;
import com.proit.app.dict.model.dictitem.DictDataItem;
import com.proit.app.dict.service.DictDataService;
import com.proit.app.dict.service.DictService;
import com.proit.app.dict.service.export.DictExportService;
import com.proit.app.dict.service.imp.ImportCsvService;
import com.proit.app.dict.service.imp.ImportJsonService;
import com.proit.app.dict.api.model.dto.DictDto;
import com.proit.app.dict.api.model.dto.data.DictItemDto;
import com.proit.app.dict.api.model.dto.data.request.CreateDictItemRequest;
import com.proit.app.dict.api.model.dto.data.request.DeleteDictItemRequest;
import com.proit.app.dict.api.model.dto.data.request.UpdateDictItemRequest;
import com.proit.app.dict.api.model.dto.request.BasicSearchRequest;
import com.proit.app.dict.api.model.dto.request.ExportDictRequest;
import com.proit.app.dict.api.model.dto.request.SearchRequest;
import com.proit.app.dict.api.service.remote.RemoteDictDataServiceV2;
import com.proit.app.attachment.utils.AttachmentUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.InputStream;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/remote/dicts/v2")
@Tag(name = "remote-dict-data-endpoint-v2", description = "Методы для работы с данными справочников (Remote)")
public class RemoteDictDataEndpointV2 implements RemoteDictDataServiceV2
{
	private final DictConverter dictConverter;
	private final DictItemConverter dictItemConverter;

	private final DictService dictService;
	private final DictDataService dictDataService;
	private final ImportCsvService importCsvService;
	private final ImportJsonService importJsonService;
	private final DictExportService dictExportService;

	@Override
	@Operation(summary = "Получение схемы справочника по id.")
	@GetMapping("/{dictId}/scheme")
	public ApiResponse<DictDto> scheme(@Parameter(description = "Идентификатор справочника") @PathVariable String dictId)
	{
		return ApiResponse.of(dictConverter.convert(dictService.getById(dictId)));
	}

	@Override
	@Operation(summary = "Получение записей справочника по фильтру.")
	@PostMapping("/{dictId}/list")
	public ApiResponse<List<DictItemDto>> getByFilter(@PathVariable String dictId, @RequestBody @Valid SearchRequest request,
	                                                  @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable)
	{
		var result = dictDataService.getByFilter(dictId, request.getFields(), request.getQuery(), pageable);

		return ApiResponse.of(dictItemConverter.convert(result));
	}

	@Override
	@Operation(summary = "Получение списка ИД справочника по фильтру без пагинации.")
	@PostMapping("/{dictId}/ids")
	public ApiResponse<List<String>> getIdsByFilter(@PathVariable String dictId, @RequestBody @Valid BasicSearchRequest request)
	{
		var result = dictDataService.getIdsByFilter(dictId, request.getQuery());

		return ApiResponse.of(result.getContent());
	}

	@Override
	@Operation(summary = "Получение записей справочника по списку id.")
	@PostMapping("/{dictId}/byIds")
	public ApiResponse<List<DictItemDto>> getByIds(@PathVariable String dictId, @RequestBody List<String> ids)
	{
		var result = dictDataService.getByIds(dictId, ids);

		return ApiResponse.of(dictItemConverter.convert(result));
	}

	@Override
	@Operation(summary = "Добавление записи в справочник.")
	@PostMapping("/{dictId}/create")
	public ApiResponse<DictItemDto> create(@PathVariable String dictId, @RequestBody @Valid CreateDictItemRequest request)
	{
		var result = dictDataService.create(DictDataItem.of(dictId, request.getData()));

		return ApiResponse.of(dictItemConverter.convert(result));
	}

	@Override
	@Operation(summary = "Обновление записи в справочнике.")
	@PostMapping("/{dictId}/update")
	public ApiResponse<DictItemDto> update(@PathVariable String dictId, @RequestBody @Valid UpdateDictItemRequest request)
	{
		var result = dictDataService.update(request.getItemId(), DictDataItem.of(dictId, request.getData()), request.getVersion());

		return ApiResponse.of(dictItemConverter.convert(result));
	}

	@Override
	@Operation(summary = "Удаление записи в справочнике (soft delete).")
	@PostMapping("/{dictId}/delete")
	public ApiResponse<?> delete(@PathVariable String dictId, @RequestBody @Valid DeleteDictItemRequest request)
	{
		dictDataService.delete(dictId, request.getItemId(), request.isDeleted(), request.getReason(), request.getVersion());

		return ApiResponse.ok();
	}

	@Override
	@Operation(description = "Проверка существования записи в справочнике по идентификатору.")
	@PostMapping("/{dictId}/existsById")
	public ApiResponse<Boolean> existsById(@PathVariable String dictId, @RequestParam String itemId)
	{
		return ApiResponse.of(dictDataService.existsById(dictId, itemId));
	}

	@Override
	@Operation(description = "Проверка существования записи в справочнике по фильтру.")
	@PostMapping("/{dictId}/existsByFilter")
	public ApiResponse<Boolean> existsByFilter(@PathVariable String dictId, @RequestParam String filter)
	{
		return ApiResponse.of(dictDataService.existsByFilter(dictId, filter));
	}

	@Override
	@Operation(description = "Получение количества записей в справочнике по фильтру.")
	@PostMapping("/{dictId}/countByFilter")
	public ApiResponse<Long> countByFilter(@PathVariable String dictId, @RequestParam String filter)
	{
		return ApiResponse.of(dictDataService.countByFilter(dictId, filter));
	}

	@Override
	@Operation(summary = "Импорт CSV в справочник.")
	@PostMapping(value = "/{dictId}/import", consumes = "text/csv")
	public OkResponse importCsv(@PathVariable String dictId, @RequestBody InputStream inputStream)
	{
		importCsvService.importDict(dictId, inputStream);

		return ApiResponse.ok();
	}

	@Override
	@Operation(summary = "Импорт JSON в справочник.")
	@PostMapping(value = "/{dictId}/import", consumes = MediaType.APPLICATION_JSON_VALUE)
	public OkResponse importJson(@PathVariable String dictId, @RequestBody InputStream inputStream)
	{
		importJsonService.importDict(dictId, inputStream);

		return ApiResponse.ok();
	}

	@Override
	@Operation(summary = "Экспорт элементов справочника.")
	@PostMapping("/{dictId}/export")
	public ResponseEntity<Resource> export(@PathVariable String dictId, @RequestBody @Valid ExportDictRequest request)
	{
		var exportedResource = dictExportService.exportToResource(dictId, request.getFormat(), request.getItemIds());

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, AttachmentUtils.buildContentDisposition(false, exportedResource.getFilename(), exportedResource.getFilename()))
				.body(exportedResource.getResource());
	}
}
