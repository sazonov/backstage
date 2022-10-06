package com.proit.app.conversion.dto.template;

import com.proit.app.conversion.dto.AbstractConverter;
import com.proit.app.model.domain.DocumentTemplate;
import com.proit.app.model.dto.template.DocumentTemplateDto;
import org.springframework.stereotype.Component;

@Component
public class DocumentTemplateConverter extends AbstractConverter<DocumentTemplate, DocumentTemplateDto>
{
	@Override
	public DocumentTemplateDto convert(DocumentTemplate source)
	{
		var target = new DocumentTemplateDto();

		target.setId(source.getId());
		target.setName(source.getName());
		target.setAttachmentId(source.getAttachmentId());

		return target;
	}
}
