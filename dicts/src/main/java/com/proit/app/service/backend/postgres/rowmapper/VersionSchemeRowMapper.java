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

import com.proit.app.domain.VersionScheme;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.proit.app.model.versionscheme.VersionSchemeColumnName.*;

public class VersionSchemeRowMapper implements RowMapper<VersionScheme>
{
	@Override
	public VersionScheme mapRow(ResultSet rs, int rowNum) throws SQLException
	{
		return VersionScheme.builder()
				.id(rs.getString(ID.getName()))
				.version(rs.getString(VERSION.getName()))
				.script(rs.getString(SCRIPT.getName()))
				.checksum(rs.getString(CHECKSUM.getName()))
				.installed(rs.getTimestamp(INSTALLED.getName()) != null ? rs.getTimestamp(INSTALLED.getName()).toLocalDateTime() : null)
				.build();
	}
}
