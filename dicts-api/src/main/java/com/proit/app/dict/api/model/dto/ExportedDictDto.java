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

package com.proit.app.dict.api.model.dto;

import com.proit.app.dict.api.model.dto.data.DictItemDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "Данные экспорта справочника")
public class ExportedDictDto
{
	@Schema(description = "Версия экспортируемого формата")
	private Integer formatVersion;

	@Schema(description = "Описание экспортируемого справочника")
	private DictDto scheme;

	@Schema(description = "Список экспортируемых значений")
	private List<DictItemDto> items;
}
