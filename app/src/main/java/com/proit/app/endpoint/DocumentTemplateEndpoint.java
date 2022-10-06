package com.proit.app.endpoint;

import com.proit.app.conversion.dto.template.DocumentTemplateConverter;
import com.proit.app.model.TemplateContext;
import com.proit.app.model.api.ApiResponse;
import com.proit.app.model.dto.template.DocumentTemplateDto;
import com.proit.app.model.dto.template.request.SearchDocumentTemplateRequest;
import com.proit.app.model.other.template.DocumentTemplateFilter;
import com.proit.app.service.DocumentTemplateService;
import com.proit.app.service.attachment.AttachmentService;
import com.proit.app.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "document-template-endpoint", description = "Методы для работы с шаблонами документов.")
@RestController
@RequiredArgsConstructor
@ConditionalOnBean(AttachmentService.class)
@RequestMapping("/api/documentTemplate")
public class DocumentTemplateEndpoint
{
	private final DocumentTemplateService documentTemplateService;

	private final DocumentTemplateConverter documentTemplateConverter;

	@Operation(summary = "Добавление шаблона.")
	@PostMapping(value = "/create", consumes = MediaType.ALL_VALUE)
	public ApiResponse<DocumentTemplateDto> create(@RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType, @RequestParam String fileName,
	                                               @RequestBody byte[] data)
	{
		var template = documentTemplateService.create(fileName, contentType, SecurityUtils.getCurrentUserId(), data);

		return ApiResponse.of(documentTemplateConverter.convert(template));
	}

	@Operation(summary = "Получение списка шаблонов по фильтру")
	@PostMapping("/getByFilter")
	public ApiResponse<List<DocumentTemplateDto>> getByFilter(@RequestBody SearchDocumentTemplateRequest request, @PageableDefault Pageable pageable)
	{
		var filter = DocumentTemplateFilter.builder()
				.name(request.getName())
				.attachmentIds(request.getAttachmentIds())
				.build();

		return ApiResponse.of(documentTemplateConverter.convert(documentTemplateService.getByFilter(filter, pageable)));
	}

	@Operation(summary = "Создание печатной формы по шаблону")
	@PostMapping(value = "/generate")
	public ApiResponse<String> generate(@RequestParam String documentTemplateId,
	                                    @RequestBody TemplateContext context)
	{
		var generatedAttachmentId = documentTemplateService.generate(documentTemplateId, SecurityUtils.getCurrentUserId(), context);

		return ApiResponse.of(generatedAttachmentId);
	}
}
