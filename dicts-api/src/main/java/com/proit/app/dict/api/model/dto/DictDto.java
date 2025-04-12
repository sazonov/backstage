/*
 *    Copyright 2019-2024 the original author or authors.
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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@Schema(description = "Описание схемы справочника")
public class DictDto
{
	@Schema(description = "ID справочника")
	private String id;

	@Schema(description = "Название справочника")
	private String name;

	@Builder.Default
	@Schema(description = "Список допустимых полей справочника")
	private List<DictFieldDto> fields = new ArrayList<>();

	@Builder.Default
	@Schema(description = "Список enum'ов справочника")
	private List<DictEnumDto> enums = new ArrayList<>();

	@Schema(description = "Название разрешения для чтения данных справочника")
	private String viewPermission;

	@Schema(description = "Название разрешения для изменения данных справочника")
	private String editPermission;

	@Schema(description = "Флаг удаления")
	private LocalDateTime deleted;

	@Schema(description = "Место хранения справочника")
	private String engine;
}
