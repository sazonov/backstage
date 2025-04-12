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

package com.proit.app.dict.service.migration;

import com.proit.app.dict.configuration.backend.provider.DictSchemeBackendProvider;
import com.proit.app.dict.configuration.backend.provider.DictTransactionBackendProvider;
import com.proit.app.dict.domain.Dict;
import com.proit.app.dict.exception.dict.DictException;
import com.proit.app.dict.model.dict.DictTransactionItem;
import com.proit.app.dict.service.backend.DictTransactionBackend;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class DictTransactionProvider
{
	public static final String DICT_SCHEME_ID = "dict";

	private final AtomicBoolean activeTransaction = new AtomicBoolean(false);
	private final AtomicReference<Set<DictTransactionItem>> transactionData = new AtomicReference<>();

	private final DictTransactionBackend dictTransactionBackend;

	private final DictSchemeBackendProvider schemeBackendProvider;
	private final DictTransactionBackendProvider dictTransactionBackendProvider;

	public DictTransactionProvider(@Lazy DictTransactionBackend dictTransactionBackend,
	                               @Lazy DictSchemeBackendProvider schemeBackendProvider,
	                               @Lazy DictTransactionBackendProvider dictTransactionBackendProvider)
	{
		this.dictTransactionBackend = dictTransactionBackend;
		this.schemeBackendProvider = schemeBackendProvider;
		this.dictTransactionBackendProvider = dictTransactionBackendProvider;
	}

	public void begin()
	{
		if (isActiveTransaction())
		{
			throw new DictException("Транзакция DDL уже открыта.");
		}

		if (doActiveTransaction())
		{
			setNewTransactionData();
		}
		else
		{
			throw new DictException("Транзакция DDL уже открыта.");
		}
	}

	public void commit()
	{
		if (!isActiveTransaction())
		{
			throw new DictException("Отсутствует транзакция DDL.");
		}

		getAffectedDicts().forEach(it -> schemeBackendProvider.getBackendByEngineName(it.getDictEngine().getName())
				.deleteDictSchemeById(it.getCopiedDictId()));

		setNewTransactionData();
		setInactiveTransaction();
	}

	public void rollback()
	{
		if (!isActiveTransaction())
		{
			throw new DictException("Отсутствует транзакция DDL.");
		}

		rollbackChanges();

		setNewTransactionData();
		setInactiveTransaction();
	}

	public void addAffectedDict(DictTransactionItem transactionItem)
	{
		getAffectedDicts().add(transactionItem);
	}

	public boolean isActiveTransaction()
	{
		return activeTransaction.get();
	}

	public void addDictTransactionItem(Dict dict, boolean schemeUsed)
	{
		if (!isActiveTransaction())
		{
			return;
		}

		if (schemeUsed && notContainsOriginal(DICT_SCHEME_ID))
		{
			var dictCopyId = dictTransactionBackend.copyDict(DICT_SCHEME_ID);

			var dictEngine = dictTransactionBackend.getEngine().getDictEngine();

			addAffectedDict(DictTransactionItem.of(DICT_SCHEME_ID, dictCopyId,
					dictEngine, true));
		}

		if (dict == null)
		{
			return;
		}

		var originalDictId = dict.getId();

		if (notContainsOriginal(originalDictId))
		{
			var dictEngine = dict.getEngine();

			var dictCopyId = dictTransactionBackendProvider.getBackendByEngineName(dictEngine.getName())
					.copyDict(originalDictId);

			addAffectedDict(DictTransactionItem.of(originalDictId, dictCopyId,
					dictEngine, schemeUsed));
		}
	}

	private Set<DictTransactionItem> getAffectedDicts()
	{
		return transactionData.get();
	}

	private void setNewTransactionData()
	{
		transactionData.set(new LinkedHashSet<>());
	}

	private void setInactiveTransaction()
	{
		activeTransaction.set(false);
	}

	private boolean doActiveTransaction()
	{
		return activeTransaction.compareAndSet(false, true);
	}

	private boolean notContainsOriginal(String originalDictId)
	{
		return getAffectedDicts().stream()
				.map(DictTransactionItem::getOriginalDictId)
				.noneMatch(originalDictId::equals);
	}

	private void rollbackChanges()
	{
		getAffectedDicts().forEach(it -> {
			if (StringUtils.equals(it.getOriginalDictId(), DICT_SCHEME_ID))
			{
				dictTransactionBackend.rollbackChanges(it);
			}
			else
			{
				dictTransactionBackendProvider.getBackendByEngineName(it.getDictEngine().getName())
						.rollbackChanges(it);
			}
		});
	}
}
