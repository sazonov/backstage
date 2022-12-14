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

package com.proit.app.endpoint;

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
@RequestMapping("/api/dicts")
@Tag(name = "dict-data-endpoint", description = "???????????? ?????? ???????????? ?? ?????????????? ????????????????????????")
public class DictDataEndpoint
{
	private final DictConverter dictConverter;
	private final DictItemConverter dictItemConverter;

	private final DictService dictService;
	private final DictDataService dictDataService;
	private final ImportCsvService importCsvService;
	private final ImportJsonService importJsonService;
	private final DictExportService dictExportService;

	@Operation(summary = "?????????????????? ?????????? ?????????????????????? ???? id.")
	@GetMapping("/{dictId}/scheme")
	public ApiResponse<DictDto> scheme(@Parameter(description = "?????????????????????????? ??????????????????????") @PathVariable String dictId)
	{
		return ApiResponse.of(dictConverter.convert(dictService.getById(dictId)));
	}

	@Operation(summary = "?????????????????? ?????????????? ?????????????????????? ???? ??????????????.")
	@PostMapping("/{dictId}/list")
	public ApiResponse<List<DictItemDto>> getByFilter(@PathVariable String dictId, @RequestBody @Valid SearchRequest request,
	                                                  @PageableDefault Pageable pageable)
	{
		var result = dictDataService.getByFilter(dictId, request.getFields(), request.getQuery(), pageable);

		return ApiResponse.of(dictItemConverter.convert(result));
	}

	@Operation(summary = "?????????????????? ?????????????? ?????????????????????? ???? ???????????? id.")
	@PostMapping("/{dictId}/byIds")
	public ApiResponse<List<DictItemDto>> getByIds(@PathVariable String dictId, @RequestBody List<String> ids)
	{
		var result = dictDataService.getByIds(dictId, ids);

		return ApiResponse.of(dictItemConverter.convert(result));
	}

	@Transactional
	@Operation(summary = "???????????????????? ???????????? ?? ????????????????????.")
	@PostMapping("/{dictId}/create")
	public ApiResponse<DictItemDto> create(@PathVariable String dictId, @RequestBody @Valid CreateDictItemRequest request)
	{
		var result = dictDataService.create(dictId, request.getData());

		return ApiResponse.of(dictItemConverter.convert(result));
	}

	@Transactional
	@Operation(summary = "???????????????????? ???????????? ?? ??????????????????????.")
	@PostMapping("/{dictId}/update")
	public ApiResponse<DictItemDto> update(@PathVariable String dictId, @RequestBody @Valid UpdateDictItemRequest request)
	{
		var result = dictDataService.update(dictId, request.getItemId(), request.getVersion(), request.getData());

		return ApiResponse.of(dictItemConverter.convert(result));
	}

	@Transactional
	@Operation(summary = "???????????????? ???????????? ?? ?????????????????????? (soft delete).")
	@PostMapping("/{dictId}/delete")
	public ApiResponse<?> delete(@PathVariable String dictId, @RequestBody @Valid DeleteDictItemRequest request)
	{
		dictDataService.delete(dictId, request.getItemId(), request.isDeleted(), request.getReason());

		return ApiResponse.ok();
	}

	@Operation(summary = "???????????? CSV ?? ????????????????????.")
	@PostMapping(value = "/{dictId}/import", consumes = "text/csv")
	public OkResponse importCsv(@PathVariable String dictId, @RequestBody InputStream inputStream)
	{
		importCsvService.importDict(dictId, inputStream);

		return ApiResponse.ok();
	}

	@Operation(summary = "???????????? JSON ?? ????????????????????.")
	@PostMapping(value = "/{dictId}/import", consumes = MediaType.APPLICATION_JSON_VALUE)
	public OkResponse importJson(@PathVariable String dictId, @RequestBody InputStream inputStream)
	{
		importJsonService.importDict(dictId, inputStream);

		return ApiResponse.ok();
	}

	@Operation(summary = "?????????????? ?????????????????? ??????????????????????.")
	@PostMapping("/{dictId}/export")
	public ResponseEntity<?> export(@PathVariable String dictId, @RequestBody @Valid ExportDictRequest request)
	{
		var data = dictExportService.exportToResource(dictId, request.getFormat(), request.getItemIds());

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, AttachmentUtils.buildContentDisposition(false, data.getDescription(), data.getDescription()))
				.contentType(MediaType.parseMediaType(MimeTypeUtils.getMimeTypeByFilename(data.getDescription())))
				.body(data);
	}
}
