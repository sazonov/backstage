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

package com.proit.app.dict.api.model.dto.data;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Schema(description = "Запись в справочнике")
public class DictItemDto
{
	private String id;

	@Builder.Default
	@Schema(description = "Пользовательские поля")
	@JsonSerialize(using = DictDataJsonSerializer.class)
	@JsonDeserialize(using = DictDataDeserializer.class)
	private Map<String, Object> data = new HashMap<>();

	@Builder.Default
	@Schema(description = "История изменений записи")
	private List<Map<String, Object>> history = new ArrayList<>();

	@Schema(description = "Текущая версия записи")
	private Long version;

	private LocalDateTime created;

	private LocalDateTime updated;

	private LocalDateTime deleted;

	@Schema(description = "Причина удаления")
	private String deletionReason;
}
