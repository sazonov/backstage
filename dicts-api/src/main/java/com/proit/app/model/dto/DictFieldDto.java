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

package com.proit.app.model.dto;

import com.proit.app.domain.DictFieldType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Описание поля справочника")
public class DictFieldDto
{
	@Schema(description = "ID поля")
	private String id;

	@Schema(description = "Название")
	private String name;

	@Schema(description = "Тип данных")
	private DictFieldType type;

	@Schema(description = "Маппинг на другой справочник (для FK)")
	private DictFieldNameDto dictRef;

	@Schema(description = "ID enum'а (для полей типа \"enum\")")
	private String enumId;

	@Schema(description = "Возможность хранить список значений")
	private boolean multivalued;

	@Schema(description = "Обязательность поля")
	private boolean required;

	@Schema(description = "Минимальный размер (для числовых полей), минимальная длина строки (для строк)")
	private Number minSize;

	@Schema(description = "Максимальный размер (для числовых полей), максимальная длина строки (для строк)")
	private Number maxSize;
}
