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

package com.proit.app.service.backend.postgres;

import com.proit.app.exception.migration.transaction.DictCopyException;
import com.proit.app.exception.migration.transaction.DictRollbackException;
import com.proit.app.model.dict.DictTransactionItem;
import com.proit.app.service.backend.DictBackend;
import com.proit.app.service.backend.DictTransactionBackend;
import com.proit.app.service.backend.Engine;
import com.proit.app.service.migration.DictTransactionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PostgresDictTransactionBackend extends AbstractPostgresBackend implements DictTransactionBackend
{
	private final DictBackend dictBackend;
	private final PostgresDictSchemeBackend postgresSchemeBackend;

	@Override
	public Engine getEngine()
	{
		return postgresEngine;
	}

	@Override
	public String copyDict(String dictId)
	{
		return transactionWithResult(() -> copiedDict(dictId), dictId, DictCopyException::new);
	}

	@Override
	public void rollbackChanges(DictTransactionItem transactionItem)
	{
		transactionWithoutResult(() -> rollback(transactionItem), transactionItem.getOriginalDictId(), DictRollbackException::new);
	}

	private String copiedDict(String dictId)
	{
		var copyDictId = dictId + "_clone";

		var copySchema = """
				do
				$$
				    declare
				        rec record;
				    begin
				        execute format(
				                'create table %s (like %s including all)',
				                '${copy}', '${original}');
				        for rec in
				            select oid, conname
				            from pg_constraint
				            where contype = 'f'
				              and conrelid = '${original}'::regclass
				            loop
				                execute format(
				                        'alter table %s add constraint %s %s',
				                        '${copy}',
				                        replace(rec.conname, '${original}', '${copy}'),
				                        pg_get_constraintdef(rec.oid));
				            end loop;
				    end
				$$
				""";

		var parameterMap = Map.of(
				"copy", copyDictId,
				"original", wordMap(dictId).get(dictId).getQuotedIfKeyword()
		);

		var schemaSql = sqlWithParameters(copySchema, parameterMap);
		var insertDataSql = sqlWithParameters("insert into ${copy} (select * from ${original})", parameterMap);

		jdbc.update(String.join(";", List.of(schemaSql, insertDataSql)), new EmptySqlParameterSource());

		return copyDictId;
	}

	private void rollback(DictTransactionItem transactionItem)
	{
		var original = transactionItem.getOriginalDictId();
		var copy = transactionItem.getCopiedDictId();

		postgresSchemeBackend.deleteDictSchemeById(original);

		if (original.equals(DictTransactionProvider.DICT_SCHEME_ID) || dictBackend.existsById(original))
		{
			postgresSchemeBackend.renameDictSchemeById(copy, original);
		}
		else
		{
			postgresSchemeBackend.deleteDictSchemeById(copy);
		}
	}
}
