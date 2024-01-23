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

package com.proit.app.dict.service.advice;

import com.proit.app.attachment.service.AttachmentService;
import com.proit.app.dict.api.domain.DictFieldType;
import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.domain.DictField;
import com.proit.app.dict.domain.DictItem;
import com.proit.app.dict.model.dictitem.DictDataItem;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class BindingDictDataServiceAdvice implements DictDataServiceAdvice
{
	public static final String DICTS_ATTACHMENT_TYPE_TEMPLATE = "DICTS_%s_%s_%s";

	private static final String DEFAULT_USER_ID = "-1";

	private final AttachmentService attachmentService;

	@Override
	public void handleAfterCreate(Dict dict, DictItem item)
	{
		handleAllAttachments(dict, item.getData(), item.getId(), attachmentService::bindAttachments);
	}

	@Override
	public void handleUpdate(Dict dict, DictItem oldItem, DictDataItem dictDataItem)
	{
		var dictId = dictDataItem.getDictId();
		var dataItemMap = dictDataItem.getDataItemMap();

		var updatedAttachmentFields = getAttachmentDictFieldIds(dict)
				.stream()
				.filter(field -> !Objects.equals(oldItem.getData().get(field.getId()), dataItemMap.get(field.getId())))
				.toList();

		updatedAttachmentFields.forEach(field -> {
			var oldFieldAttachmentIds = getAttachmentIdsFromField(oldItem.getData().get(field.getId()), field);
			var newFieldAttachmentIds = getAttachmentIdsFromField(dictDataItem.getDataItemMap().get(field.getId()), field);

			var deletedAttachmentIds = CollectionUtils.subtract(oldFieldAttachmentIds, newFieldAttachmentIds);
			var addedAttachmentIds = CollectionUtils.subtract(newFieldAttachmentIds, oldFieldAttachmentIds);

			var attachmentType = DICTS_ATTACHMENT_TYPE_TEMPLATE.formatted(dictId, oldItem.getId(), field.getId());
			attachmentService.releaseAttachments(deletedAttachmentIds, DEFAULT_USER_ID, attachmentType, oldItem.getId());
			attachmentService.bindAttachments(addedAttachmentIds, DEFAULT_USER_ID, attachmentType, oldItem.getId());
		});
	}

	@Override
	public void handleDelete(Dict dict, DictItem item, boolean deleted)
	{
		var attachmentIds = getAttachmentIds(dict, item);

		if (deleted)
		{
			attachmentIds.forEach(attachmentId -> handleAllAttachments(dict, item.getData(), item.getId(), attachmentService::releaseAttachments));
		}
		else
		{
			attachmentIds.forEach(attachmentId -> handleAllAttachments(dict, item.getData(), item.getId(), attachmentService::bindAttachments));
		}
	}

	//TODO: перевести мапу (doc) на DictDataItem
	private void handleAllAttachments(Dict dict, Map<String, Object> doc, String id, Consumer4<Collection<String>, String, String, String> action)
	{
		getAttachmentDictFieldIds(dict)
				.stream()
				.filter(dictField -> dictField.isRequired() || doc.get(dictField.getId()) != null)
				.forEach(field -> action.accept(getAttachmentIdsFromField(doc.get(field.getId()), field),
						DEFAULT_USER_ID, DICTS_ATTACHMENT_TYPE_TEMPLATE.formatted(dict.getId(), id, field.getId()), id));
	}

	private List<String> getAttachmentIds(Dict dict, DictItem dictItem)
	{
		return getAttachmentDictFieldIds(dict)
				.stream()
				.filter(field -> dictItem.getData().get(field.getId()) != null)
				.flatMap(field -> getAttachmentIdsFromField(dictItem.getData().get(field.getId()), field).stream())
				.toList();
	}

	@SuppressWarnings("unchecked")
	private List<String> getAttachmentIdsFromField(Object value, DictField dictField)
	{
		if (value == null)
		{
			return List.of();
		}

		if (dictField.isMultivalued())
		{
			return ((List<String>) value);
		}

		return List.of((String) value);
	}

	private List<DictField> getAttachmentDictFieldIds(Dict dict)
	{
		return dict.getFields()
				.stream()
				.filter(it -> it.getType().equals(DictFieldType.ATTACHMENT))
				.toList();
	}

	// FIXME: 14.09.2023 Скопировано из reactor.function.Consumer4. Переписать решение нормально
	@FunctionalInterface
	private interface Consumer4<A, B, C, D>
	{
		void accept(A t1, B t2, C t3, D t4);
	}
}
