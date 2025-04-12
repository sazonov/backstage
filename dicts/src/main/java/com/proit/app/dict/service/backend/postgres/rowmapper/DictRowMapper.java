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

package com.proit.app.dict.service.backend.postgres.rowmapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.domain.DictEngine;
import com.proit.app.dict.model.dict.DictColumnName;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;

public class DictRowMapper implements RowMapper<Dict>
{
	public Dict mapRow(ResultSet rs, int rowNum)
	{
		var mapper = new ObjectMapper();

		try
		{
			var deletedTimestamp = rs.getTimestamp(DictColumnName.DELETED.getName());
			var deleted = deletedTimestamp != null
					? deletedTimestamp.toLocalDateTime()
					: null;

			return Dict.builder()
				.id(rs.getString(DictColumnName.ID.getName()))
				.name(rs.getString(DictColumnName.NAME.getName()))
				.fields(mapper.readValue(rs.getString(DictColumnName.FIELDS.getName()), new TypeReference<>() { }))
				.indexes(mapper.readValue(rs.getString(DictColumnName.INDEXES.getName()), new TypeReference<>() { }))
				.constraints(mapper.readValue(rs.getString(DictColumnName.CONSTRAINTS.getName()), new TypeReference<>() { }))
				.enums(mapper.readValue(rs.getString(DictColumnName.ENUMS.getName()), new TypeReference<>() { }))
				.viewPermission(rs.getString(DictColumnName.VIEW_PERMISSION.getName()))
				.editPermission(rs.getString(DictColumnName.EDIT_PERMISSION.getName()))
				.deleted(deleted)
				.engine(new DictEngine(rs.getString(DictColumnName.ENGINE.getName())))
				.build();
		}
		catch (Exception e)
		{
			throw new RuntimeException("При маппинге строки в модель Dict, произошла ошибка: %s".formatted(e));
		}
	}
}
