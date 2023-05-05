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

package com.proit.app.service.mapping;

import com.proit.app.constant.ServiceFieldConstants;
import com.proit.app.domain.DictFieldName;
import com.proit.app.model.dictitem.FieldDataItem;
import org.springframework.stereotype.Component;

@Component
public class FieldDataItemDictFieldNameMapper implements DictDataMapper<FieldDataItem, DictFieldName>
{
	@Override
	public DictFieldName map(FieldDataItem fieldItem)
	{
		var withCorrectFieldId = withCorrectFieldId(fieldItem);

		return new DictFieldName(withCorrectFieldId.getDictId(), withCorrectFieldId.getFieldItem());
	}

	//TODO: Временно (для сохранения обратной совместимости).
	// Переместить в валидацию переданных клиентом сервисных для адаптера полей с оформлением соответствующей ошибкой.
	@Deprecated(forRemoval = true)
	private FieldDataItem withCorrectFieldId(FieldDataItem fieldItem)
	{
		return FieldDataItem.builder()
				.dictId(fieldItem.getDictId())
				.fieldItem(fieldItem.getFieldItem().replaceAll(ServiceFieldConstants._ID, ServiceFieldConstants.ID))
				.build();
	}
}
