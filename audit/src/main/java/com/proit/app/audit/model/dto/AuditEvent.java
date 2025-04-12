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

package com.proit.app.audit.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.proit.app.audit.model.domain.AuditPropertiesField;
import com.proit.app.audit.model.domain.AuditPropertiesProperty;
import com.proit.app.model.other.date.DateConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AuditEvent implements Serializable
{
	@Schema(description = "Тип события")
	private String type;

	@Schema(description = "Идентификатор объекта, к которому относится событие")
	private String objectId;

	@Schema(description = "Инициатор события (для системных событий - null)")
	private String userId;

	@Schema(description = "Дата фиксации")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DateConstants.ISO_OFFSET_DATE_TIME_MS_FORMAT)
	private ZonedDateTime date;

	@Schema(description = "Успешность действия, связанного с событием")
	private boolean success;

	@Schema(description = "Изменённые поля/атрибуты объекта")
	private List<AuditPropertiesField> fields = new ArrayList<>();

	@Schema(description = "Дополнительные параметры события")
	private List<AuditPropertiesProperty> properties = new ArrayList<>();
}
