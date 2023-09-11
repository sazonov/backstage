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

package com.proit.app.service.backend.postgres.rowmapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proit.app.domain.Dict;
import com.proit.app.domain.DictEngine;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;

import static com.proit.app.model.dict.DictColumnName.*;

public class DictRowMapper implements RowMapper<Dict>
{
	public Dict mapRow(ResultSet rs, int rowNum)
	{
		var mapper = new ObjectMapper();

		try
		{
			return Dict.builder()
				.id(rs.getString(ID.getName()))
				.name(rs.getString(NAME.getName()))
				.fields(mapper.readValue(rs.getString(FIELDS.getName()), new TypeReference<>() { }))
				.indexes(mapper.readValue(rs.getString(INDEXES.getName()), new TypeReference<>() { }))
				.constraints(mapper.readValue(rs.getString(CONSTRAINTS.getName()), new TypeReference<>() { }))
				.enums(mapper.readValue(rs.getString(ENUMS.getName()), new TypeReference<>() { }))
				.viewPermission(rs.getString(VIEW_PERMISSION.getName()))
				.editPermission(rs.getString(EDIT_PERMISSION.getName()))
				.deleted(rs.getTimestamp(DELETED.getName()) != null ? rs.getTimestamp(DELETED.getName()).toLocalDateTime() : null)
				.engine(new DictEngine(rs.getString(ENGINE.getName())))
				.build();
		}
		catch (Exception e)
		{
			throw new RuntimeException("При маппинге строки в модель Dict, произошла ошибка: %s".formatted(e));
		}
	}
}
