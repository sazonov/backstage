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

package com.proit.app.attachment.endpoint;

import com.proit.app.api.model.ApiResponse;
import com.proit.app.api.model.OkResponse;
import com.proit.app.attachment.configuration.properties.AttachmentProperties;
import com.proit.app.attachment.conversion.dto.AttachmentConverter;
import com.proit.app.attachment.model.dto.AttachmentDto;
import com.proit.app.attachment.service.AttachmentService;
import com.proit.app.attachment.utils.AttachmentUtils;
import com.proit.app.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "attachment-endpoint", description = "Методы для работы с вложениями.")
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(AttachmentProperties.ACTIVATION_PROPERTY)
@RequestMapping("/api/attachment")
public class AttachmentEndpoint
{
	private final AttachmentService attachmentService;
	private final AttachmentConverter attachmentConverter;

	@GetMapping("/info")
	@Operation(summary = "Получение информации о вложении по идентификатору.")
	public ApiResponse<AttachmentDto> getInfo(@RequestParam String id)
	{
		return ApiResponse.of(attachmentConverter.convert(attachmentService.getAttachment(id)));
	}

	@GetMapping("/getAllInfo")
	@Operation(summary = "Получение информации о вложении по коллекции идентификаторов.")
	public ApiResponse<List<AttachmentDto>> getAllInfo(@RequestParam List<String> ids)
	{
		return ApiResponse.of(attachmentConverter.convert(attachmentService.getAttachments(ids)));
	}

	@GetMapping(value = {"/download", "/get"}, consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@Operation(summary = "Получение данных вложения по идентификатору.")
	public ResponseEntity<?> download(@RequestParam String id,
	                                  @Parameter(description = "Позволяет получить данные вложения в inline режиме")
	                                  @RequestParam(defaultValue = "false") boolean view)
	{
		var attachment = attachmentService.getAttachment(id);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, AttachmentUtils.buildContentDisposition(view, attachment.getId(), attachment.getFileName()))
				.contentType(MediaType.parseMediaType(attachment.getMimeType()))
				.body(attachmentService.getAttachmentData(id));
	}

	@PostMapping(value = "/upload", consumes = MediaType.ALL_VALUE)
	public ApiResponse<String> upload(@RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType, @RequestParam String fileName,
	                                  @RequestBody byte[] data)
	{
		var attachment = attachmentService.addAttachment(fileName, contentType, SecurityUtils.getCurrentUserId(), data);

		return ApiResponse.of(attachment.getId());
	}

	@PostMapping("/syncStores")
	@Operation(summary = "Сервисная операция для миграции вложений из одного типа хранилища в другое.")
	public OkResponse syncStores(@RequestParam AttachmentProperties.StoreType source, @RequestParam AttachmentProperties.StoreType target)
	{
		attachmentService.syncAttachmentStores(source, target);

		return ApiResponse.ok();
	}
}
