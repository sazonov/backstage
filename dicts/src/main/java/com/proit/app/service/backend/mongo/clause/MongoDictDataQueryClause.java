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

package com.proit.app.service.backend.mongo.clause;

import com.proit.app.domain.DictFieldName;
import com.proit.app.model.mongo.query.MongoQueryContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.proit.app.constant.ServiceFieldConstants._ID;

@Component
public class MongoDictDataQueryClause
{
	private static final String LOOKUP_SUFFIX = "@join";

	public void addSelectFields(List<DictFieldName> requiredFields, Query query)
	{
		var requiredBasicFields = requiredFields.stream()
				.filter(it -> it.getDictId() == null)
				.map(DictFieldName::getFieldId)
				.toList();

		if (!requiredBasicFields.contains("*"))
		{
			var fields = query.fields();

			requiredBasicFields.forEach(fields::include);
		}
	}

	public void addJoin(List<DictFieldName> requiredFields, Set<String> joinFields, MongoQueryContext queryContext,
	                    List<AggregationOperation> operations, Pageable mongoPageable, String dictId)
	{
		queryContext.getParticipantDictIds()
				.stream()
				.filter(Predicate.not(dictId::equals))
				.forEach(joinFields::add);

		if (mongoPageable.getSort().isSorted())
		{
			var refDictMap = requiredFields.stream()
					.filter(it -> it.getDictId() != null)
					.collect(Collectors.toMap(DictFieldName::getDictId, Function.identity()));

			//TODO: актуализировать после написания контракта под MongoPageable/PostgresPageable
			mongoPageable.getSort()
					.stream()
					.map(Sort.Order::getProperty)
					.filter(it -> refDictMap.containsKey(StringUtils.substringBefore(it, ".")))
					.map(it -> refDictMap.get(StringUtils.substringBefore(it, ".")))
					.map(DictFieldName::getDictId)
					.forEach(joinFields::add);
		}

		operations.addAll(buildLookups(joinFields));
	}

	public Sort buildSort(Sort sort, Set<String> joinFields)
	{
		var orders = joinFields.stream()
				.map(field -> sort.stream()
						.filter(order -> order.getProperty().startsWith(field))
						.findFirst()
						.map(order -> {
							var property = StringUtils.substringBefore(order.getProperty(), ".")
									+ LOOKUP_SUFFIX + "." + StringUtils.substringAfter(order.getProperty(), ".");

							return Sort.by(order.getDirection(), property);
						})
						.orElse(Sort.unsorted()))
				.reduce(Sort::and)
				.orElse(sort);

		var singleSort = orders.stream()
				.map(Sort.Order::getProperty)
				.allMatch(it -> it.startsWith(_ID));

		if (orders.isSorted() && singleSort)
		{
			return orders;
		}

		return orders.stream()
				.filter(Predicate.not(it -> it.getProperty().startsWith(_ID)))
				.map(Sort::by)
				.findFirst()
				.orElse(sort)
				.and(Sort.by(Sort.Direction.ASC, _ID));
	}

	public void addPageable(List<AggregationOperation> operations, Pageable mongoPageable)
	{
		operations.addAll(buildPaging(mongoPageable));
	}

	private List<AggregationOperation> buildLookups(Set<String> subFiledToJoin)
	{
		final var foreignFieldAlias = "foreignId";

		return subFiledToJoin.stream()
				.<AggregationOperation>mapMulti((field, downstream) -> {
					var foreignId = Aggregation.addFields()
							.addField(foreignFieldAlias)
							.withValue(ConvertOperators.ToObjectId.toObjectId("$" + field))
							.build();
					var lookup = Aggregation.lookup(field, foreignFieldAlias, _ID, field + LOOKUP_SUFFIX);

					downstream.accept(foreignId);
					downstream.accept(lookup);
				})
				.toList();
	}

	private List<AggregationOperation> buildPaging(Pageable pageable)
	{
		return List.of(Aggregation.skip(pageable.getOffset()), Aggregation.limit(pageable.getPageSize()));
	}
}
