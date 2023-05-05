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
import com.proit.app.model.dto.DictDto;
import com.proit.app.model.dto.DictEnumDto;
import com.proit.app.model.dto.request.CreateDictEnumRequest;
import com.proit.app.model.dto.request.CreateDictRequest;
import com.proit.app.model.dto.request.DeleteDictRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "remote-dict-service", description = "Методы для работы со схемами справочников.")
@FeignClient(value = RemoteServices.DICTS, path = "/api/remote/dicts", contextId = "remoteDictService")
public interface RemoteDictService
{
	@Schema(description = "Получение схемы справочника по идентификатору.")
	@GetMapping("/get")
	ApiResponse<DictDto> get(@RequestParam String id);

	@Schema(description = "Получение схем всех существующих справочников.")
	@GetMapping("/list")
	ApiResponse<List<DictDto>> list();

	@Schema(description = "Добавление справочника.")
	@PostMapping("/create")
	ApiResponse<DictDto> create(@RequestBody CreateDictRequest request);

	@Schema(description = "Добавление enum в справочник.")
	@PostMapping("/enum/create")
	ApiResponse<DictEnumDto> createEnum(@RequestBody CreateDictEnumRequest request, @RequestParam String dictId);

	@Schema(description = "Обновление enum в справочнике.")
	@PostMapping("/enum/update")
	ApiResponse<DictEnumDto> updateEnum(@RequestBody CreateDictEnumRequest request, @RequestParam String dictId);

	@Schema(description = "Удаление enum в справочнике.")
	@PostMapping("/enum/delete")
	ApiResponse<?> deleteEnum(@RequestParam String enumId, @RequestParam String dictId);

	@Schema(description = "Обновление справочника.")
	@PostMapping("/update")
	ApiResponse<DictDto> update(@RequestBody CreateDictRequest request);

	@Schema(description = "Удаление справочника (soft delete).")
	@PostMapping("/delete")
	ApiResponse<?> delete(@RequestBody DeleteDictRequest request);
}
