/*
 *
 *  Copyright 2019-2023 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.proit.app.service.mapping.mongo;

import com.proit.app.constant.ServiceFieldConstants;
import com.proit.app.domain.DictField;
import com.proit.app.domain.DictFieldType;
import com.proit.app.domain.DictItem;
import com.proit.app.model.dictitem.MongoDictDataItem;
import com.proit.app.service.DictService;
import com.proit.app.service.mapping.DictDataMapper;
import com.proit.app.utils.StreamCollectors;
import lombok.RequiredArgsConstructor;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.proit.app.constant.ServiceFieldConstants.*;
import static com.proit.app.constant.ServiceFieldConstants.HISTORY;

@Component
@RequiredArgsConstructor
public class MongoDictDataItemDictItemMapper implements DictDataMapper<MongoDictDataItem, DictItem>
{
	private final DictService dictService;

	@Override
	public DictItem map(MongoDictDataItem dataItem)
	{
		if (dataItem.getMongoData() == null)
		{
			return null;
		}

		var dictData = (Map<String, Object>) dataItem.getMongoData();

		var fields = dictService.getDataFieldsByDictId(dataItem.getDictId());

		var availableFieldIds = fields.stream()
				.map(DictField::getId)
				.collect(Collectors.toSet());

		var dictFieldMap = fields.stream().collect(Collectors.toMap(DictField::getId, Function.identity()));

		var refFieldIds = fields.stream()
				.filter(it -> it.getType() == DictFieldType.DICT)
				.map(DictField::getId)
				.collect(Collectors.toSet());

		var mappedDictData = dictData.entrySet()
				.stream()
				.filter(entry -> !ServiceFieldConstants.getServiceSchemeFields().contains(entry.getKey()))
				.filter(entry -> availableFieldIds.contains(entry.getKey()))
				.peek(entry -> setValue(dictFieldMap, refFieldIds, entry))
				.collect(StreamCollectors.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));

		var created = (Date) dictData.get(CREATED);
		var updated = (Date) dictData.get(UPDATED);
		var deleted = (Date) dictData.get(DELETED);
		var deletionReason = (String) dictData.get(DELETION_REASON);

		return DictItem.builder()
				.id(dictData.get(_ID) instanceof String s ? s : ((ObjectId) dictData.get(_ID)).toString())
				.version((Long) dictData.get(VERSION))
				.history((List<Map<String, Object>>) dictData.get(HISTORY))
				.created(created == null ? null : mapToLocalDateTime(created))
				.deleted(deleted == null ? null : mapToLocalDateTime(deleted))
				.deletionReason(deleted == null ? null : deletionReason)
				.updated(updated == null ? null : mapToLocalDateTime(updated))
				.data(mappedDictData)
				.build();
	}

	private void setValue(Map<String, DictField> dictFieldMap, Set<String> refFieldIds, Map.Entry<String, Object> entry)
	{
		entry.setValue(refFieldIds.contains(entry.getKey()) && entry.getValue() instanceof Map
				? map(MongoDictDataItem.of(dictFieldMap.get(entry.getKey()).getDictRef().getDictId(), entry.getValue()))
				: castDictField(entry.getValue()));
	}

	/**
	 * Для Decimal поля конвертируем исходный тип в BigDecimal
	 */
	private Object castDictField(Object o)
	{
		if (o instanceof Decimal128 decimal128)
		{
			return decimal128.bigDecimalValue();
		}

		return o;
	}

	private LocalDateTime mapToLocalDateTime(Date value)
	{
		return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	}
}
