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

package com.proit.app.service.remote;

import com.proit.app.constant.RemoteServices;
import com.proit.app.model.api.ApiResponse;
import com.proit.app.model.api.OkResponse;
import com.proit.app.model.dto.DictDto;
import com.proit.app.model.dto.data.DictItemDto;
import com.proit.app.model.dto.data.request.CreateDictItemRequest;
import com.proit.app.model.dto.data.request.DeleteDictItemRequest;
import com.proit.app.model.dto.data.request.UpdateDictItemRequest;
import com.proit.app.model.dto.request.ExportDictRequest;
import com.proit.app.model.dto.request.SearchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.InputStream;
import java.util.List;

@Tag(name = "remote-dict-data-service", description = "Методы для работы с данными справочников")
@FeignClient(value = RemoteServices.DICTS, path = "/api/remote/dicts", contextId = "remoteDictDataService")
public interface RemoteDictDataService
{
	@Schema(description = "Получение схемы справочника по id.")
	@GetMapping("/{dictId}/scheme")
	ApiResponse<DictDto> scheme(@Parameter(description = "Идентификатор справочника") @PathVariable String dictId);

	@Schema(description = "Получение записей справочника по фильтру.")
	@PostMapping("/{dictId}/list")
	ApiResponse<List<DictItemDto>> getByFilter(@PathVariable String dictId, @RequestBody SearchRequest request,
	                                           @SpringQueryMap Pageable pageable, @RequestParam String userId);

	@Schema(description = "Получение записей справочника по списку id.")
	@PostMapping("/{dictId}/byIds")
	ApiResponse<List<DictItemDto>> getByIds(@PathVariable String dictId, @RequestBody List<String> ids, @RequestParam String userId);

	@Schema(description = "Добавление записи в справочник.")
	@PostMapping("/{dictId}/create")
	ApiResponse<DictItemDto> create(@PathVariable String dictId, @RequestBody CreateDictItemRequest request, @RequestParam String userId);

	@Schema(description = "Обновление записи в справочнике.")
	@PostMapping("/{dictId}/update")
	ApiResponse<DictItemDto> update(@PathVariable String dictId, @RequestBody UpdateDictItemRequest request, @RequestParam String userId);

	@Schema(description = "Удаление записи в справочнике (soft delete).")
	@PostMapping("/{dictId}/delete")
	ApiResponse<?> delete(@PathVariable String dictId, @RequestBody DeleteDictItemRequest request, @RequestParam String userId);

	@Schema(description = "Проверка существования записи в справочнике по идентификатору.")
	@PostMapping("/{dictId}/existsById")
	ApiResponse<Boolean> existsById(@PathVariable String dictId, @RequestParam String itemId, @RequestParam String userId);

	@Schema(description = "Проверка существования записи в справочнике по фильтру.")
	@PostMapping("/{dictId}/existsByFilter")
	ApiResponse<Boolean> existsByFilter(@PathVariable String dictId, @RequestParam String filter, @RequestParam String userId);

	@Schema(description = "Получение количества записей в справочнике по фильтру.")
	@PostMapping("/{dictId}/countByFilter")
	ApiResponse<Long> countByFilter(@PathVariable String dictId, @RequestParam String filter, @RequestParam String userId);

	@Operation(summary = "Импорт CSV в справочник.")
	@PostMapping(value = "/{dictId}/import", consumes = "text/csv")
	OkResponse importCsv(@PathVariable String dictId, @RequestBody InputStream inputStream, @RequestParam String userId);

	@Operation(summary = "Импорт JSON в справочник.")
	@PostMapping(value = "/{dictId}/import", consumes = MediaType.APPLICATION_JSON_VALUE)
	OkResponse importJson(@PathVariable String dictId, @RequestBody InputStream inputStream, @RequestParam String userId);

	@Operation(summary = "Экспорт элементов справочника.")
	@PostMapping("/{dictId}/export")
	ResponseEntity<Resource> export(@PathVariable String dictId, @RequestBody @Valid ExportDictRequest request, @RequestParam String userId);
}
