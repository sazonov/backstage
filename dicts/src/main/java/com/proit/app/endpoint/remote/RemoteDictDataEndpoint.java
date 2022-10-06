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
import com.proit.app.conversion.dto.data.DictItemConverter;
import com.proit.app.model.api.ApiResponse;
import com.proit.app.model.api.OkResponse;
import com.proit.app.model.dto.DictDto;
import com.proit.app.model.dto.data.DictItemDto;
import com.proit.app.model.dto.data.request.CreateDictItemRequest;
import com.proit.app.model.dto.data.request.DeleteDictItemRequest;
import com.proit.app.model.dto.data.request.UpdateDictItemRequest;
import com.proit.app.model.dto.request.ExportDictRequest;
import com.proit.app.model.dto.request.SearchRequest;
import com.proit.app.service.DictDataService;
import com.proit.app.service.DictService;
import com.proit.app.service.export.DictExportService;
import com.proit.app.service.imp.ImportCsvService;
import com.proit.app.service.imp.ImportJsonService;
import com.proit.app.service.remote.RemoteDictDataService;
import com.proit.app.utils.AttachmentUtils;
import com.proit.app.utils.MimeTypeUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.InputStream;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/remote/dicts")
@Tag(name = "remote-dict-data-endpoint", description = "Методы для работы с данными справочников (Remote)")
public class RemoteDictDataEndpoint implements RemoteDictDataService
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
	                                                  @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable, @RequestParam String userId)
	{
		var result = dictDataService.getByFilter(dictId, request.getFields(), request.getQuery(), pageable, userId);

		return ApiResponse.of(dictItemConverter.convert(result));
	}

	@Override
	@Operation(summary = "Получение записей справочника по списку id.")
	@PostMapping("/{dictId}/byIds")
	public ApiResponse<List<DictItemDto>> getByIds(@PathVariable String dictId, @RequestBody List<String> ids, @RequestParam String userId)
	{
		var result = dictDataService.getByIds(dictId, ids, userId);

		return ApiResponse.of(dictItemConverter.convert(result));
	}

	@Override
	@Transactional
	@Operation(summary = "Добавление записи в справочник.")
	@PostMapping("/{dictId}/create")
	public ApiResponse<DictItemDto> create(@PathVariable String dictId, @RequestBody @Valid CreateDictItemRequest request, @RequestParam String userId)
	{
		var result = dictDataService.create(dictId, request.getData(), userId);

		return ApiResponse.of(dictItemConverter.convert(result));
	}

	@Override
	@Transactional
	@Operation(summary = "Обновление записи в справочнике.")
	@PostMapping("/{dictId}/update")
	public ApiResponse<DictItemDto> update(@PathVariable String dictId, @RequestBody @Valid UpdateDictItemRequest request, @RequestParam String userId)
	{
		var result = dictDataService.update(dictId, request.getItemId(), request.getVersion(), request.getData(), userId);

		return ApiResponse.of(dictItemConverter.convert(result));
	}

	@Override
	@Transactional
	@Operation(summary = "Удаление записи в справочнике (soft delete).")
	@PostMapping("/{dictId}/delete")
	public ApiResponse<?> delete(@PathVariable String dictId, @RequestBody @Valid DeleteDictItemRequest request, @RequestParam String userId)
	{
		dictDataService.delete(dictId, request.getItemId(), request.isDeleted(), userId);

		return ApiResponse.ok();
	}

	@Override
	@Operation(description = "Проверка существования записи в справочнике по идентификатору.")
	@PostMapping("/{dictId}/existsById")
	public ApiResponse<Boolean> existsById(@PathVariable String dictId, @RequestParam String itemId, @RequestParam String userId)
	{
		return ApiResponse.of(dictDataService.existsById(dictId, itemId, userId));
	}

	@Override
	@Operation(description = "Проверка существования записи в справочнике по фильтру.")
	@PostMapping("/{dictId}/existsByFilter")
	public ApiResponse<Boolean> existsByFilter(@PathVariable String dictId, @RequestParam String filter, @RequestParam String userId)
	{
		return ApiResponse.of(dictDataService.existsByFilter(dictId, filter, userId));
	}

	@Override
	@Operation(description = "Получение количества записей в справочнике по фильтру.")
	@PostMapping("/{dictId}/countByFilter")
	public ApiResponse<Long> countByFilter(@PathVariable String dictId, @RequestParam String filter, @RequestParam String userId)
	{
		return ApiResponse.of(dictDataService.countByFilter(dictId, filter, userId));
	}

	@Override
	@Operation(summary = "Импорт CSV в справочник.")
	@PostMapping(value = "/{dictId}/import", consumes = "text/csv")
	public OkResponse importCsv(@PathVariable String dictId, @RequestBody InputStream inputStream, @RequestParam String userId)
	{
		importCsvService.importDict(dictId, inputStream, userId);

		return ApiResponse.ok();
	}

	@Override
	@Operation(summary = "Импорт JSON в справочник.")
	@PostMapping(value = "/{dictId}/import", consumes = MediaType.APPLICATION_JSON_VALUE)
	public OkResponse importJson(@PathVariable String dictId, @RequestBody InputStream inputStream, @RequestParam String userId)
	{
		importJsonService.importDict(dictId, inputStream, userId);

		return ApiResponse.ok();
	}

	@Override
	@Operation(summary = "Экспорт элементов справочника.")
	@PostMapping("/{dictId}/export")
	public ResponseEntity<?> export(@PathVariable String dictId, @RequestBody @Valid ExportDictRequest request, @RequestParam String userId)
	{
		var data = dictExportService.exportToResource(dictId, request.getFormat(), request.getItemIds(), userId);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, AttachmentUtils.buildContentDisposition(false, data.getDescription(), data.getDescription()))
				.contentType(MediaType.parseMediaType(MimeTypeUtils.getMimeTypeByFilename(data.getDescription())))
				.body(data);
	}
}
