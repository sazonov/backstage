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

package com.proit.app.service.backend.mongo;

import com.proit.app.domain.DictFieldName;
import com.proit.app.domain.DictFieldType;
import com.proit.app.domain.DictItem;
import com.proit.app.exception.ObjectNotFoundException;
import com.proit.app.exception.backend.PrepareMongoPageableException;
import com.proit.app.exception.dict.DictException;
import com.proit.app.model.mongo.MongoClause;
import com.proit.app.model.mongo.query.MongoQueryContext;
import com.proit.app.service.backend.DictBackend;
import com.proit.app.service.backend.DictDataBackend;
import com.proit.app.service.backend.Engine;
import com.proit.app.service.backend.mongo.clause.MongoDictDataQueryClause;
import com.proit.app.service.query.MongoTranslator;
import com.proit.app.service.query.QueryParser;
import com.proit.app.service.query.ast.Empty;
import com.proit.app.service.query.ast.QueryExpression;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.proit.app.constant.ServiceFieldConstants._ID;

@Component
@RequiredArgsConstructor
public class MongoDictDataBackend extends AbstractMongoBackend implements DictDataBackend
{
	private static final Pattern ID_PATTERN = Pattern.compile("^(id)");
	private static final Pattern JOINED_ID_PATTERN = Pattern.compile("(\\.id)");

	private final DictBackend dictBackend;

	private final MongoDictDataBackendMapper backendMapper;

	private final MongoDictDataQueryClause queryClause;

	private final QueryParser queryParser;
	private final MongoTranslator mongoTranslator;

	@Override
	public Engine getEngine()
	{
		return mongoEngine;
	}

	@Override
	public DictItem getById(String dictId, String id, List<DictFieldName> requiredFields)
	{
		var query = "%s = '%s'".formatted(_ID, id);

		return getByFilter(dictId, requiredFields, queryParser.parse(query), Pageable.unpaged())
				.getContent()
				.stream()
				.findFirst()
				.orElseThrow(() -> new ObjectNotFoundException(DictItem.class, "dictId: %s, itemId: %s".formatted(dictId, id)));
	}

	@Override
	public List<DictItem> getByIds(String dictId, List<String> ids, List<DictFieldName> requiredFields)
	{
		var itemIds = ids.stream()
				.map(it -> "'" + it + "'")
				.collect(Collectors.joining(", "));

		var filtersQuery = "%s in (%s)".formatted(_ID, itemIds);

		return getByFilter(dictId, requiredFields, queryParser.parse(filtersQuery), Pageable.unpaged()).getContent();
	}

	@Override
	public Page<DictItem> getByFilter(String dictId, List<DictFieldName> requiredFields, QueryExpression queryExpression, Pageable pageable)
	{
		var queryContext = mongoTranslator.process(dictBackend.getDictById(dictId), queryExpression);

		var mongoClause = completedMongoClauses(requiredFields, new HashSet<>(), new Query(),
				queryContext, new LinkedList<>(), pageable, dictId);

		var documents = getDocuments(dictId, queryExpression, mongoClause.getJoinField(),
				mongoClause.getOperations(), mongoClause.getQuery(), queryContext);

		var pagedDocuments = documentsWithPaginated(dictId, documents, mongoClause.getQuery(), mongoClause.getPageable());

		buildReferenceDataIfExists(dictId, requiredFields, pagedDocuments);

		return pagedDocuments.map(document -> backendMapper.mapFrom(dictId, document));
	}

	@Override
	public boolean existsById(String dictId, String itemId)
	{
		var query = Query.query(Criteria.where(_ID).is(itemId));

		return mongoTemplate.exists(query, dictId);
	}

	@Override
	public boolean existsByFilter(String dictId, QueryExpression queryExpression)
	{
		return getByFilter(dictId, List.of(new DictFieldName(null, "*")), queryExpression, Pageable.unpaged())
				.hasContent();
	}

	@Override
	public DictItem create(String dictId, DictItem dictItem)
	{
		addTransactionData(dictId, false);

		return Optional.of(backendMapper.mapTo(dictId, dictItem))
				.map(document -> mongoTemplate.save(document, dictId))
				.map(document -> backendMapper.mapFrom(dictId, document))
				.orElseThrow(() -> new DictException("Ошибка при создании пользовательских данных справочника %s".formatted(dictId)));
	}

	//TODO: рассмотреть возможность замыкание на create
	@Override
	public List<DictItem> createMany(String dictId, List<DictItem> dictItems)
	{
		addTransactionData(dictId, false);

		return dictItems.stream()
				.map(item -> backendMapper.mapTo(dictId, item))
				.collect(Collectors.collectingAndThen(Collectors.toList(), docs -> mongoTemplate.insert(docs, dictId)))
				.stream()
				.map(document -> backendMapper.mapFrom(dictId, document))
				.toList();
	}

	@Override
	public DictItem update(String dictId, String itemId, DictItem dictItem, long version)
	{
		addTransactionData(dictId, false);

		var query = Query.query(Criteria.where(_ID).is(itemId));

		update(dictId, dictItem, query);

		return backendMapper.mapFrom(dictId, mongoTemplate.findOne(query, Document.class, dictId));
	}

	@Override
	public void delete(String dictId, DictItem dictItem)
	{
		addTransactionData(dictId, false);

		var query = Query.query(Criteria.where(_ID).is(dictItem.getId()));

		update(dictId, dictItem, query);
	}

	@Override
	public void deleteAll(String dictId, List<DictItem> dictItems)
	{
		addTransactionData(dictId, false);

		dictItems.forEach(it -> update(dictId, it, Query.query(Criteria.where(_ID).is(it.getId()))));
	}

	@Override
	public long countByFilter(String dictId, QueryExpression queryExpression)
	{
		return getByFilter(dictId, List.of(new DictFieldName(null, "*")), queryExpression, Pageable.unpaged())
				.getTotalElements();
	}

	private MongoClause completedMongoClauses(List<DictFieldName> requiredFields, HashSet<String> joinFields, Query query,
	                                          MongoQueryContext queryContext, LinkedList<AggregationOperation> operations, Pageable pageable, String dictId)
	{
		query.collation(Collation.of("ru").strength(Collation.ComparisonLevel.secondary()));

		queryClause.addSelectFields(requiredFields, query);
		queryClause.addJoin(requiredFields, joinFields, queryContext, operations, pageable, dictId);

		var mongoPageable = pageable.isUnpaged() ? pageable : buildMongoPageable(pageable);

		if (mongoPageable.isPaged())
		{
			var orders = queryClause.buildSort(mongoPageable.getSort(), joinFields);

			mongoPageable = PageRequest.of(mongoPageable.getPageNumber(), mongoPageable.getPageSize(), orders);

			operations.add(Aggregation.sort(mongoPageable.getSort()));
			queryClause.addPageable(operations, mongoPageable);

			query.with(mongoPageable);
		}

		return new MongoClause(query, mongoPageable, joinFields, operations);
	}

	private List<Document> getDocuments(String dictId, QueryExpression queryExpression, Set<String> joinFields,
	                                    List<AggregationOperation> operations, Query query, MongoQueryContext queryContext)
	{
		if (queryExpression instanceof Empty && joinFields.isEmpty())
		{
			return mongoTemplate.find(query, Document.class, dictId);
		}

		if (!joinFields.isEmpty())
		{
			return mongoTemplate.aggregate(Aggregation.newAggregation(operations), dictId, Document.class)
					.getMappedResults();
		}

		return mongoTemplate.find(query.addCriteria(queryContext.getCriteria()), Document.class, dictId);
	}

	private void buildReferenceDataIfExists(String dictId, List<DictFieldName> requiredFields, Page<Document> documents)
	{
		var isExistRefs = requiredFields.stream()
				.map(DictFieldName::getDictId)
				.filter(Objects::nonNull)
				.anyMatch(it -> !dictId.equals(it));

		if (isExistRefs)
		{
			var requiredRefFields = requiredFields.stream()
					.filter(field -> field.getDictId() != null)
					.filter(field -> !dictId.equals(field.getDictId()))
					.collect(Collectors.toList());

			buildReferenceData(dictId, requiredRefFields, documents);
		}
	}

	private void update(String dictId, DictItem dictItem, Query query)
	{
		var old = getById(dictId, dictItem.getId(), List.of(new DictFieldName(null, "*")));

		var changes = getChanges(dictId, dictItem, old);

		if (!changes.isEmpty())
		{
			mongoTemplate.upsert(query, buildUpdateClauses(changes), dictId);
		}
	}

	private Document getChanges(String dictId, DictItem dictItem, DictItem oldItem)
	{
		var document = backendMapper.mapTo(dictId, dictItem);
		var oldDocument = backendMapper.mapTo(dictId, oldItem);

		return document.entrySet()
				.stream()
				.filter(it -> !it.getKey().equals(_ID))
				.filter(it -> (it.getValue() == null && oldDocument.get(it.getKey()) != null) || (it.getValue() != null && !it.getValue().equals(oldDocument.get(it.getKey()))))
				.collect(Document::new, (doc, entry) -> doc.append(entry.getKey(), entry.getValue()), Document::putAll);
	}

	private Update buildUpdateClauses(Document document)
	{
		var updateClauses = new Update();

		document.forEach(updateClauses::set);

		return updateClauses;
	}

	private Page<Document> documentsWithPaginated(String dictId, List<Document> dictItems, Query query, Pageable pageable)
	{
		return PageableExecutionUtils.getPage(
				dictItems,
				pageable,
				() -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), dictId));
	}

	private void buildReferenceData(String dictId, List<DictFieldName> requiredFields, Page<Document> itemList)
	{
		var refFieldMap = dictBackend.getDictById(dictId)
				.getFields()
				.stream()
				.filter(field -> field.getType() == DictFieldType.DICT)
				.collect(Collectors.toMap(Function.identity(), field -> getRefIds(field.getId(), itemList)));

		refFieldMap.forEach(((dictField, refIds) -> {
			var requiredFieldIds = requiredFields.stream()
					.filter(it -> it.getDictId().equals(dictField.getDictRef().getDictId()))
					.map(DictFieldName::getFieldId)
					.collect(Collectors.toSet());

			var refQuery = new Query();

			if (!requiredFieldIds.contains("*"))
			{
				var refFields = refQuery.fields();

				requiredFieldIds.forEach(refFields::include);
			}

			refQuery.addCriteria(Criteria.where(_ID).in(refIds));

			var refItems = mongoTemplate.find(refQuery, Document.class, dictField.getDictRef().getDictId())
					.stream()
					.collect(Collectors.toMap(document -> document.get(_ID) instanceof String s ? s : document.get(_ID).toString(), Function.identity()));

			matchRefsDocument(itemList, dictField.getId(), refItems);
		}));
	}

	private List<String> getRefIds(String fieldId, Page<Document> items)
	{
		return items.stream()
				.flatMap(document -> (document.get(fieldId) instanceof List list)
						? list.stream()
						: Stream.of((String) document.get(fieldId)))
				.distinct()
				.toList();
	}

	private void matchRefsDocument(Page<Document> itemList, String fieldId, Map<String, Document> refDocument)
	{
		itemList.stream()
				.filter(document -> document.get(fieldId) != null)
				.forEach(document -> {
					if (document.get(fieldId) instanceof List list)
					{
						list = list.stream()
								.map(it -> refDocument.get((String) document.get((String) it)))
								.toList();

						document.put(fieldId, list);
						return;
					}

					document.put(fieldId, refDocument.get((String) document.get(fieldId)));
				});
	}

	//TODO: провести рефакторинг MongoPageable, сделать контракт с PostgresPageable
	private Pageable buildMongoPageable(Pageable pageable)
	{
		Predicate<String> idMatches = property -> ID_PATTERN.matcher(property).matches();
		Predicate<String> joinedIdMatches = property -> JOINED_ID_PATTERN.matcher(property).matches();

		Function<String, String> replaceAllId = property -> property.replaceAll(ID_PATTERN.pattern() + "|" + JOINED_ID_PATTERN.pattern(), _ID);

		var isReplaceId = pageable.getSort()
				.stream()
				.map(Sort.Order::getProperty)
				.anyMatch(idMatches.or(joinedIdMatches));

		if (isReplaceId)
		{
			var orders = pageable.getSort()
					.stream()
					.map(order -> Sort.by(order.getDirection(), idMatches.or(joinedIdMatches).test(order.getProperty())
							? replaceAllId.apply(order.getProperty())
							: order.getProperty()))
					.reduce(Sort::and)
					.orElseThrow(() -> new PrepareMongoPageableException(pageable.getSort().toString()));

			return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), orders);
		}

		return pageable;
	}
}
