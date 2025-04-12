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

package com.proit.app.dict.api.model.dto.request;

import com.proit.app.dict.api.constant.ExportedDictFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Schema(description = "Запрос для экспорта справочника")
public class ExportDictRequest
{
	@NotNull
	@Schema(description = "Формат файла для экспорта")
	private ExportedDictFormat format;

	@Schema(description = "Частичный экспорт из справочника только указанных элементов")
	private List<String> itemIds;
}
