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

package com.proit.app.service.advice;

import com.proit.app.domain.DictField;
import com.proit.app.domain.DictFieldType;
import com.proit.app.domain.DictItem;
import com.proit.app.model.dictitem.DictDataItem;
import com.proit.app.service.DictService;
import com.proit.app.service.attachment.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.function.Consumer4;

import java.util.Collection;
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
		handleAttachment(dictId, item.getData(), item.getId(), attachmentService::bindAttachments);
	}

	@Override
	public void handleUpdate(DictItem oldItem, DictDataItem dictDataItem)
	{
		var dictId = dictDataItem.getDictId();
		var dataItemMap = dictDataItem.getDataItemMap();

		var updatedAttachmentFieldIds = getAttachmentDictFieldIds(dictId)
				.stream()
				.map(DictField::getId)
				.filter(id -> !Objects.equals(oldItem.getData().get(id), dataItemMap.get(id)))
				.toList();

		if (!updatedAttachmentFieldIds.isEmpty())
		{
			handleAttachment(dictId, oldItem.getData(), oldItem.getId(), attachmentService::releaseAttachments);
			handleAttachment(dictId, dataItemMap, oldItem.getId(), attachmentService::bindAttachments);
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
			attachmentIds.forEach(attachmentId -> handleAttachment(dictId, item.getData(), item.getId(), attachmentService::bindAttachments));
		}
	}

	//TODO: перевести мапу (doc) на DictDataItem
	private void handleAttachment(String dictId, Map<String, Object> doc, String id, Consumer4<Collection<String>, String, String, String> action)
	{
		getAttachmentDictFieldIds(dictId)
				.stream()
				.filter(dictField -> dictField.isRequired() || doc.get(dictField.getId()) != null)
				.forEach(field -> action.accept(getAttachmentIdsFromField(doc.get(field.getId()), field),
						"-1", DICTS_ATTACHMENT_TYPE.formatted(dictId, id, field.getId()), id));
	}

	private List<String> getAttachmentIds(String dictId, DictItem dictItem)
	{
		return getAttachmentDictFieldIds(dictId)
				.stream()
				.filter(field -> dictItem.getData().get(field.getId()) != null)
				.flatMap(field -> getAttachmentIdsFromField(dictItem.getData().get(field.getId()), field).stream())
				.toList();
	}

	@SuppressWarnings("unchecked")
	private List<String> getAttachmentIdsFromField(Object value, DictField dictField)
	{
		if (dictField.isMultivalued())
		{
			return ((List<String>) value);
		}

		return List.of((String) value);
	}

	private List<DictField> getAttachmentDictFieldIds(String dictId)
	{
		return dictService.getById(dictId)
				.getFields()
				.stream()
				.filter(it -> it.getType().equals(DictFieldType.ATTACHMENT))
				.toList();
	}
}
