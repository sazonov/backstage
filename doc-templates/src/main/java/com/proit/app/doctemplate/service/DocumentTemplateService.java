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

package com.proit.app.doctemplate.service;

import com.proit.app.doctemplate.model.domain.DocumentTemplate;
import com.proit.app.exception.AppException;
import com.proit.app.doctemplate.model.TemplateContext;
import com.proit.app.model.other.exception.ApiStatusCodeImpl;
import com.proit.app.doctemplate.model.other.DocumentTemplateFilter;
import com.proit.app.doctemplate.repository.DocumentTemplateRepository;
import com.proit.app.attachment.service.AttachmentService;
import com.proit.app.utils.DataUtils;
import com.proit.app.database.utils.StreamUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wickedsource.docxstamper.DocxStamper;
import org.wickedsource.docxstamper.DocxStamperConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnBean(AttachmentService.class)
@RequiredArgsConstructor
public class DocumentTemplateService
{
	private enum AttachmentTypes
	{
		DOCUMENT_TEMPLATE
	}

	private final NamedParameterJdbcTemplate jdbc;

	private final DocumentTemplateRepository documentTemplateRepository;

	private final AttachmentService attachmentService;

	@Transactional
	public DocumentTemplate create(@NonNull String fileName, @NonNull String mimeType, @NonNull String userId, byte[] data)
	{
		var attachment = attachmentService.addAttachment(fileName, mimeType, userId, data);

		var newTemplate = documentTemplateRepository.save(DocumentTemplate.builder()
				.attachmentId(attachment.getId())
				.name(fileName)
				.build());

		attachmentService.bindAttachment(attachment.getId(), userId, AttachmentTypes.DOCUMENT_TEMPLATE, newTemplate.getId());

		return newTemplate;
	}

	public Page<DocumentTemplate> getByFilter(DocumentTemplateFilter filter, Pageable pageable)
	{
		var selectClauses = new HashSet<String>();
		var whereClauses = new HashSet<String>();
		var parameters = new MapSqlParameterSource();

		selectClauses.add("dt.id");

		completeClauses(filter, whereClauses, parameters, pageable);

		var sql = "from document_template dt "
				+ (whereClauses.isEmpty() ? "" : " where " + String.join(" and ", whereClauses));

		var idsSql = "select distinct " + String.join(", ", selectClauses) + " " + sql
				+ (pageable.isPaged() ? " offset :offset limit :limit" : "");

		var countSql = "select count(*) " + sql;

		var ids = jdbc.queryForList(idsSql, parameters).stream()
				.map(it -> (String) it.get("id"))
				.toList();

		if (ids.isEmpty())
		{
			return DataUtils.emptyPage(pageable);
		}

		var count = jdbc.queryForObject(countSql, parameters, Long.class);

		var templates = documentTemplateRepository.findAll(ids)
				.stream()
				.sorted(StreamUtils.listOrderComparator(ids))
				.collect(Collectors.toList());

		return new PageImpl<>(templates, pageable, count);
	}

	private void completeClauses(DocumentTemplateFilter filter, HashSet<String> whereClauses, MapSqlParameterSource parameters, Pageable pageable)
	{
		if (!filter.getAttachmentIds().isEmpty())
		{
			parameters.addValue("attachmentIds", filter.getAttachmentIds());
			whereClauses.add("dt.fk_attachment in(:attachmentIds)");
		}

		if (StringUtils.isNotBlank(filter.getName()))
		{
			whereClauses.add("dt.name ilike :name");
			parameters.addValue("name", "%" + filter.getName() + "%");
		}

		if (pageable != null && pageable.isPaged())
		{
			parameters.addValue("limit", pageable.getPageSize());
			parameters.addValue("offset", pageable.getOffset());
		}
	}

	public String generate(String documentTemplateId, String currentUserId, TemplateContext context)
	{
		var documentTemplate = documentTemplateRepository.findByIdEx(documentTemplateId);
		var attachment = attachmentService.getAttachment(documentTemplate.getAttachmentId());
		var templateFile = attachmentService.getAttachmentData(documentTemplate.getAttachmentId());

		try (var inputStream = templateFile.getInputStream();
		     var outputStream = new ByteArrayOutputStream())
		{
			var config = new DocxStamperConfiguration().leaveEmptyOnExpressionError(true);
			var stamper = new DocxStamper<>(config);

			stamper.stamp(inputStream, context, outputStream);

			var filename = StringUtils.isNotBlank(context.getFilename()) ? context.getFilename() : attachment.getFileName();
			var generatedAttachment = attachmentService.addAttachment(filename, attachment.getMimeType(), currentUserId, outputStream.toByteArray());

			log.info("Создание печатной формы {} по шаблону {}.", generatedAttachment.getId(), attachment.getId());

			return generatedAttachment.getId();
		}
		catch (IOException e)
		{
			throw new AppException(ApiStatusCodeImpl.DOCUMENT_TEMPLATE_GENERATE_ERROR, e);
		}
	}
}
