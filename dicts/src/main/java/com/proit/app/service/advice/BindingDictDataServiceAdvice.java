package com.proit.app.service.advice;

import com.proit.app.domain.DictField;
import com.proit.app.domain.DictFieldType;
import com.proit.app.domain.DictItem;
import com.proit.app.service.DictService;
import com.proit.app.service.attachment.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.function.Consumer4;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class BindingDictDataServiceAdvice implements DictDataServiceAdvice
{
	private final DictService dictService;
	private final AttachmentService attachmentService;

	public static final String DICTS_ATTACHMENT_TYPE = "DICTS_%s_%s_%s";

	@Override
	public void handleAfterCreate(String dictId, DictItem item)
	{
		handleAttachment(dictId, item.getData(), item.getId(), attachmentService::bindAttachment);
	}

	@Override
	public void handleUpdate(String dictId, DictItem oldItem, Map<String, Object> updatedItem)
	{
		var updatedAttachmentFieldIds = getAttachmentDictFieldIds(dictId)
				.stream()
				.filter(id -> !Objects.equals(oldItem.getData().get(id), updatedItem.get(id)))
				.toList();

		if (!updatedAttachmentFieldIds.isEmpty())
		{
			handleAttachment(dictId, oldItem.getData(), oldItem.getId(), attachmentService::releaseAttachment);
			handleAttachment(dictId, updatedItem, oldItem.getId(), attachmentService::bindAttachment);
		}
	}

	@Override
	public void handleDelete(String dictId, DictItem item, boolean deleted)
	{
		var attachmentIds = getAttachmentIds(dictId, item);

		if (deleted)
		{
			attachmentIds.forEach(attachmentService::releaseAttachment);
		}
		else
		{
			attachmentIds.forEach(attachmentId -> handleAttachment(dictId, item.getData(), item.getId(), attachmentService::bindAttachment));
		}
	}

	private void handleAttachment(String dictId, Map<String, Object> doc, String id, Consumer4<String, String, String, String> action)
	{
		getAttachmentDictFieldIds(dictId)
				.forEach(fieldId -> action.accept((String) doc.get(fieldId), "-1",
						DICTS_ATTACHMENT_TYPE.formatted(dictId, id, fieldId), id));
	}

	private List<String> getAttachmentIds(String dictId, DictItem dictItem)
	{
		return getAttachmentDictFieldIds(dictId)
				.stream()
				.filter(field -> dictItem.getData().get(field) != null)
				.map(field -> (String) dictItem.getData().get(field))
				.toList();
	}

	private List<String> getAttachmentDictFieldIds(String dictId)
	{
		return dictService.getById(dictId)
				.getFields()
				.stream()
				.filter(it -> it.getType().equals(DictFieldType.ATTACHMENT))
				.map(DictField::getId)
				.toList();
	}
}
