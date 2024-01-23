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

package com.proit.app.dict.service.backend.postgres;

import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.domain.DictEnum;
import com.proit.app.dict.exception.dict.DictCreatedException;
import com.proit.app.dict.exception.dict.DictDeletedException;
import com.proit.app.dict.exception.dict.DictNotFoundException;
import com.proit.app.dict.exception.dict.DictUpdatedException;
import com.proit.app.dict.exception.dict.enums.EnumCreatedException;
import com.proit.app.dict.exception.dict.enums.EnumDeletedException;
import com.proit.app.dict.exception.dict.enums.EnumNotFoundException;
import com.proit.app.dict.exception.dict.enums.EnumUpdatedException;
import com.proit.app.dict.model.dict.DictColumnName;
import com.proit.app.dict.service.backend.postgres.rowmapper.DictRowMapper;
import com.proit.app.dict.service.backend.DictBackend;
import com.proit.app.dict.service.backend.Engine;
import com.proit.app.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PostgresDictBackend extends AbstractPostgresBackend implements DictBackend
{
	@Override
	public Engine getEngine()
	{
		return postgresEngine;
	}

	@Override
	public Dict getDictById(String id)
	{
		Objects.requireNonNull(id, "dictId не может быть null.");

		var sql = "select * from dict where id = :id";

		var dict = jdbc.query(sql, new MapSqlParameterSource(DictColumnName.ID.getName(), id), new DictRowMapper())
				.stream()
				.findFirst()
				.orElseThrow(() -> new DictNotFoundException(id));

		if (dict.getDeleted() != null)
		{
			throw new DictDeletedException(id);
		}

		return dict;
	}

	@Override
	public List<Dict> getAllDicts()
	{
		var sql = "select * from dict";

		return jdbc.query(sql, new DictRowMapper());
	}

	@Override
	public Dict saveDict(Dict dict)
	{
		return transactionWithResult(() -> savedDict(dict), dict.getId(), DictCreatedException::new);
	}

	//TODO: рассмотреть необходимость обновления, через сравнение актуального состояния с обновляемым
	// сейчас обновляется вся строка (кроме id)
	@Override
	public Dict updateDict(Dict dict)
	{
		return transactionWithResult(() -> updatedDict(dict), dict.getId(), DictUpdatedException::new);
	}

	@Override
	public void deleteById(String id)
	{
		transactionWithoutResult(() -> deleteDict(id), id, DictDeletedException::new);
	}

	@Override
	public void softDelete(Dict dict, LocalDateTime deleted)
	{
		addTransactionData(dict, true);

		var dictId = dict.getId();

		transactionWithoutResult(() -> softDeleteDict(dictId, deleted), dictId, DictDeletedException::new);
	}

	@Override
	public boolean existsById(String id)
	{
		var sql = "select exists(select 1 from dict where id = :id)";

		return Boolean.TRUE.equals(jdbc.queryForObject(sql, new MapSqlParameterSource(DictColumnName.ID.getName(), id), Boolean.class));
	}

	@Override
	public DictEnum createEnum(Dict dict, DictEnum dictEnum)
	{
		addTransactionData(null, true);

		return transactionWithResult(() -> createdEnum(dict, dictEnum), dict.getId(), EnumCreatedException::new);
	}

	@Override
	public DictEnum updateEnum(Dict dict, DictEnum dictEnum)
	{
		addTransactionData(null, true);

		return transactionWithResult(() -> updatedEnum(dict, dictEnum), dict.getId(), dictEnum.getId(), EnumUpdatedException::new);
	}

	@Override
	public void deleteEnum(Dict dict, String enumId)
	{
		addTransactionData(null, true);

		transactionWithoutResult(() -> deleteEnum(dict), dict.getId(), enumId, EnumDeletedException::new);
	}

	private Dict savedDict(Dict dict)
	{
		var parameterMap = new MapSqlParameterSource();

		completeClauses(parameterMap, dict);

		var sql = "insert into dict values "
				+ "(:id, :name, :fields::jsonb, :indexes::jsonb, :constraints::jsonb, :enums::jsonb, :view_permission, "
				+ ":edit_permission, :deleted, :engine)";

		jdbc.update(sql, parameterMap);

		return dict;
	}

	private Dict updatedDict(Dict dict)
	{
		var parameterMap = new MapSqlParameterSource();

		completeClauses(parameterMap, dict);

		var sql = ("update dict "
				+ "set name = :name, fields = :fields::jsonb, indexes = :indexes::jsonb, constraints = :constraints::jsonb, "
				+ "enums = :enums::jsonb, view_permission = :view_permission, edit_permission = :edit_permission, "
				+ "deleted = :deleted, engine = :engine "
				+ "where id = :id");

		jdbc.update(sql, parameterMap);

		return dict;
	}

	private void deleteDict(String id)
	{
		var sql = "delete from dict where id = :id";

		jdbc.update(sql, new MapSqlParameterSource(DictColumnName.ID.getName(), id));
	}

	private void softDeleteDict(String id, LocalDateTime deleted)
	{
		var parameterMap = new MapSqlParameterSource();

		parameterMap.addValue(DictColumnName.ID.getName(), id);
		parameterMap.addValue(DictColumnName.DELETED.getName(), deleted);

		var sql = "update dict set deleted = :deleted where id = :id";

		jdbc.update(sql, parameterMap);
	}

	private DictEnum createdEnum(Dict dict, DictEnum dictEnum)
	{
		var parameterMap = new MapSqlParameterSource();

		addParameter(parameterMap, DictColumnName.ID.getName(), dict.getId());
		addParameter(parameterMap, DictColumnName.ENUMS.getName(), JsonUtils.toJson(dict.getEnums()));

		var sql = "update dict set enums = :enums::jsonb where id = :id";

		jdbc.update(sql, parameterMap);

		return dictEnum;
	}

	private DictEnum updatedEnum(Dict dict, DictEnum dictEnum)
	{
		var oldEnum = dict.getEnums()
				.stream()
				.filter(it -> it.getId().equals(dictEnum.getId()))
				.findAny()
				.orElseThrow(() -> new EnumNotFoundException(dictEnum.getId()));

		oldEnum.setName(dictEnum.getName());
		oldEnum.setValues(dictEnum.getValues());

		var parameterMap = new MapSqlParameterSource();

		addParameter(parameterMap, DictColumnName.ID.getName(), dict.getId());
		addParameter(parameterMap, DictColumnName.ENUMS.getName(), JsonUtils.toJson(dict.getEnums()));

		var sql = "update dict set enums = :enums::jsonb where id = :id";

		jdbc.update(sql, parameterMap);

		return dictEnum;
	}

	private void deleteEnum(Dict dict)
	{
		var parameterMap = new MapSqlParameterSource();

		addParameter(parameterMap, DictColumnName.ID.getName(), dict.getId());
		addParameter(parameterMap, DictColumnName.ENUMS.getName(), JsonUtils.toJson(dict.getEnums()));

		var sql = "update dict set enums = :enums::jsonb where id = :id";

		jdbc.update(sql, parameterMap);
	}

	private void completeClauses(MapSqlParameterSource parameterMap, Dict dict)
	{
		addParameter(parameterMap, DictColumnName.ID.getName(), dict.getId());
		addParameter(parameterMap, DictColumnName.NAME.getName(), dict.getName());
		addParameter(parameterMap, DictColumnName.FIELDS.getName(), JsonUtils.toJson(dict.getFields()));
		addParameter(parameterMap, DictColumnName.INDEXES.getName(), JsonUtils.toJson(dict.getIndexes()));
		addParameter(parameterMap, DictColumnName.CONSTRAINTS.getName(), JsonUtils.toJson(dict.getConstraints()));
		addParameter(parameterMap, DictColumnName.ENUMS.getName(), JsonUtils.toJson(dict.getEnums()));
		addParameter(parameterMap, DictColumnName.VIEW_PERMISSION.getName(), dict.getViewPermission());
		addParameter(parameterMap, DictColumnName.EDIT_PERMISSION.getName(), dict.getEditPermission());
		addParameter(parameterMap, DictColumnName.DELETED.getName(), dict.getDeleted());
		addParameter(parameterMap, DictColumnName.ENGINE.getName(), dict.getEngine().getName());
	}
}
