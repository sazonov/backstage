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

package com.proit.app.model.dto.attachment;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.proit.app.model.api.ApiConstants;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Setter
public class AttachmentDto
{
	private String id;

	private String userId;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ApiConstants.API_TIMESTAMP_FORMAT)
	private ZonedDateTime created;

	private String mimeType;

	private String fileName;

	private Integer size;

	private String url;

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<AttachmentBindingDto> bindings;
}
