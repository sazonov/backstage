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

import com.proit.app.domain.Dict;
import com.proit.app.domain.DictFieldName;
import com.proit.app.domain.DictFieldType;
import com.proit.app.domain.DictItem;
import com.proit.app.exception.ObjectNotFoundException;
import com.proit.app.exception.backend.PrepareMongoPageableException;
import com.proit.app.exception.dictionary.DictConcurrentUpdateException;
import com.proit.app.exception.dictionary.DictException;
import com.proit.app.service.backend.DictBackend;
import com.proit.app.service.backend.DictDataBackend;
import com.proit.app.service.backend.Engine;
import com.proit.app.service.query.MongoTranslator;
import com.proit.app.service.query.QueryParser;
import com.proit.app.service.query.ast.Empty;
import com.proit.app.service.query.ast.QueryExpression;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
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

import static com.proit.app.constant.ServiceFieldConstants.*;

@Component
@RequiredArgsConstructor
public class MongoDictDataBackend extends CommonMongoBackend implements DictDataBackend
{
	private static final String LOOKUP_SUFFIX = "@join";
	private static final Pattern ID_PATTERN = Pattern.compile("^(id)");
	private static final Pattern JOINED_ID_PATTERN = Pattern.compile("(\\.id)");

	private final DictBackend dictBackend;

	private final MongoDictDataBackendMapper backendMapper;

	private final QueryParser queryParser;
	private final MongoTranslator mongoTranslator;

	private final MongoEngine mongoEngine;

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
		var filtersQuery = "%s in (%s)".formatted(_ID, String.join(",", ids.stream().map(it -> "'" + it + "'").toList()));

		return getByFilter(dictId, requiredFields, queryParser.parse(filtersQuery), Pageable.unpaged()).getContent();
	}

	@Override
	public Page<DictItem> getByFilter(String dictId, List<DictFieldName> requiredFields, QueryExpression queryExpression, Pageable pageable)
	{
		var requiredBasicFields = requiredFields.stream()
				.filter(it -> it.getDictId() == null)
				.map(DictFieldName::getFieldId)
				.collect(Collectors.toList());

		List<Document> result;

		//TODO: рассмотреть возможность разработки создания единного интерфейса сортировки без привязки к адаптеру
		// т.е. без императивной обработки pageable в адаптере (см. MongoDictDataBackend.buildMongoPageable())
		var customizedPageable = pageable.getSort().isEmpty() ? pageable : buildMongoPageable(pageable);

		var query = new Query().with(customizedPageable);
		query.collation(Collation.of("ru").strength(Collation.ComparisonLevel.secondary()));

		if (!requiredBasicFields.contains("*"))
		{
			var fields = query.fields();

			requiredBasicFields.forEach(fields::include);
		}

		var fieldsToJoin = new ArrayList<String>();
		var sort = customizedPageable.getSort();

		if (sort.isSorted())
		{
			sort.stream()
					.map(order -> requiredFields.stream()
							.map(DictFieldName::getDictId)
							.filter(Objects::nonNull)
							.filter(fieldId -> order.getProperty().startsWith(fieldId))
							.toList())
					.flatMap(Collection::stream)
					.forEach(fieldsToJoin::add);
		}

		if (queryExpression instanceof Empty && fieldsToJoin.isEmpty())
		{
			requiredBasicFields.remove("*");

			result = mongoTemplate.find(query, Document.class, dictId);
		}
		else if (!fieldsToJoin.isEmpty())
		{
			var operations = new LinkedList<>(buildLookups(fieldsToJoin));
			operations.add(buildSort(sort, fieldsToJoin));

			if (pageable.isPaged())
			{
				operations.addAll(buildPaging(pageable));
			}

			result = mongoTemplate.aggregate(Aggregation.newAggregation(operations), dictId, Document.class)
					.getMappedResults();
		}
		else
		{
			var criteria = mongoTranslator.process(dictBackend.getDictById(dictId), queryExpression);

			result = mongoTemplate.find(query.addCriteria(criteria), Document.class, dictId);
		}

		var itemList = getPage(dictId, result, query, pageable);

		var isExistRefs = requiredFields.stream()
				.map(DictFieldName::getDictId)
				.anyMatch(Objects::nonNull);

		if (isExistRefs)
		{
			var requiredRefFields = requiredFields.stream()
					.filter(field -> field.getDictId() != null)
					.collect(Collectors.toList());

			buildRefsData(dictId, requiredRefFields, itemList);
		}

		return itemList.map(document -> backendMapper.mapFrom(dictId, document));
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
		var criteria = mongoTranslator.process(dictBackend.getDictById(dictId), queryExpression);

		return mongoTemplate.exists(new Query().addCriteria(criteria), dictId);
	}

	@Override
	public DictItem create(String dictId, DictItem dictItem)
	{
		addToTransactionData(dictId, false);

		return Optional.of(backendMapper.mapTo(dictId, dictItem))
				.map(document -> mongoTemplate.save(document, dictId))
				.map(document -> backendMapper.mapFrom(dictId, document))
				.orElseThrow(() -> new DictException("Ошибка при создании пользовательских данных справочника %s".formatted(dictId)));
	}

	//TODO: рассмотреть возможность замыкание на create
	@Override
	public List<DictItem> createMany(String dictId, List<DictItem> dictItems)
	{
		addToTransactionData(dictId, false);

		return dictItems.stream()
				.map(item -> backendMapper.mapTo(dictId, item))
				.collect(Collectors.collectingAndThen(Collectors.toList(), docs -> mongoTemplate.insert(docs, dictId)))
				.stream()
				.map(document -> backendMapper.mapFrom(dictId, document))
				.toList();
	}

	@Override
	public DictItem update(String dictId, String itemId, long version, DictItem dictItem)
	{
		addToTransactionData(dictId, false);

		var query = Query.query(Criteria.where(_ID).is(itemId));

		var document = backendMapper.mapTo(dictId, dictItem);

		update(dictId, version, document, query);

		return backendMapper.mapFrom(dictId, mongoTemplate.findOne(query, Document.class, dictId));
	}

	@Override
	public void delete(String dictId, String itemId, boolean deleted, String reason)
	{
		addToTransactionData(dictId, false);

		var document = new Document();

		if (deleted)
		{
			document.append(DELETED, new Date());
			document.append(DELETION_REASON, StringUtils.isBlank(reason) ? null : reason);
		}
		else
		{
			document.append(DELETED, null);
		}

		var query = Query.query(Criteria.where(_ID).is(itemId));

		update(dictId, 0L, document, query);
	}

	@Override
	public void deleteAll(String dictId, boolean deleted)
	{
		addToTransactionData(dictId, false);

		var document = new Document();
		document.append(DELETED, deleted ? new Date() : null);

//		TODO: зарефакторить, сделано в лоб
		var query = new Query(Criteria.where(DELETED).is(null));
		query.fields().include("_id");

		mongoTemplate.find(query, Document.class, dictId)
				.forEach(it -> update(dictId, 0L, document, Query.query(Criteria.where(_ID).is(it.get(_ID)))));
	}

	@Override
	public long countByFilter(String dictId, QueryExpression queryExpression)
	{
		var criteria = mongoTranslator.process(dictBackend.getDictById(dictId), queryExpression);

		return mongoTemplate.count(new Query().addCriteria(criteria), dictId);
	}

	private void update(String dictId, long version, Document document, Query query)
	{
		var old = Optional.ofNullable(mongoTemplate.findOne(query, Document.class, dictId))
				.orElseThrow(() -> new ObjectNotFoundException(Dict.class, "dictId: %s, itemId: %s".formatted(dictId, query.getQueryObject().get(_ID))));

		validatePessimisticLockForUpdate(version, document, old);

		var changes = getChanges(document, old);

		if (!changes.isEmpty())
		{
			mongoTemplate.upsert(query, buildUpdateClauses(old, changes), dictId);
		}
	}

	private Document getChanges(Document document, Document old)
	{
		return document.entrySet()
				.stream()
				.filter(it -> !it.getKey().equals(_ID))
				.filter(it -> (it.getValue() == null && old.get(it.getKey()) != null) || (it.getValue() != null && !it.getValue().equals(old.get(it.getKey()))))
				.collect(Document::new, (doc, entry) -> doc.append(entry.getKey(), entry.getValue()), Document::putAll);
	}

	private Update buildUpdateClauses(Document old, Document document)
	{
		var updateClauses = new Update();

		var updateDate = new Date();
		updateClauses.set(UPDATED, updateDate);
		document.append(UPDATED, updateDate);

		updateClauses.set(VERSION, (Long) old.get(VERSION) + 1);
		document.append(VERSION, (Long) old.get(VERSION) + 1);

		document.forEach(updateClauses::set);

		var history = old.getList(HISTORY, Document.class);
		history.add(document);

		updateClauses.set(HISTORY, history);

		return updateClauses;
	}

	private void validatePessimisticLockForUpdate(long version, Document document, Document old)
	{
		if (version != (Long) old.get(VERSION) && (!document.containsKey(DELETED) || document.size() > 1)
				&& (!document.containsKey(DELETED) && !document.containsKey(DELETION_REASON) || document.size() > 2))
		{
			throw new DictConcurrentUpdateException(version, (Long) old.get(VERSION));
		}
	}

	private List<AggregationOperation> buildLookups(ArrayList<String> subFiledToJoin)
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

	private AggregationOperation buildSort(Sort sort, Collection<String> subFiledToJoin)
	{
		var correctSort = subFiledToJoin.stream()
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
				.orElse(Sort.by(_ID));

		return Aggregation.sort(correctSort);
	}

	private List<AggregationOperation> buildPaging(Pageable pageable)
	{
		return List.of(Aggregation.skip(pageable.getOffset()), Aggregation.limit(pageable.getPageSize()));
	}

	private Page<Document> getPage(String dictId, List<Document> dictItems, Query query, Pageable pageable)
	{
		return PageableExecutionUtils.getPage(
				dictItems,
				pageable,
				() -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), dictId));
	}

	private void buildRefsData(String dictId, List<DictFieldName> requiredFields, Page<Document> itemList)
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

	private List<String> getRefIds(String fieldId, Page<Document> items)
	{
		return items.stream()
				.flatMap(document -> {
					if (document.get(fieldId) instanceof List list)
					{
						return list.stream();
					}
					return Stream.of((String) document.get(fieldId));
				})
				.distinct()
				.toList();
	}

	//TODO: провести рефакторинг метода
	private Pageable buildMongoPageable(Pageable pageable)
	{
		Predicate<String> idMatches = property -> ID_PATTERN.matcher(property).matches();
		Predicate<String> joinedIdMatches = property -> JOINED_ID_PATTERN.matcher(property).matches();

		Function<String, String> replaceAllId = property -> property.replaceAll(ID_PATTERN.pattern() + "|" + JOINED_ID_PATTERN.pattern(), _ID);

		var isReplaceId = pageable.getSort()
				.stream()
				.map(Sort.Order::getProperty)
				.anyMatch(idMatches.or(joinedIdMatches));

		var isAppendId = pageable.getSort()
				.stream()
				.map(Sort.Order::getProperty)
				.noneMatch(idMatches);

		var result = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
				isAppendId ? pageable.getSort().and(Sort.by(Sort.Direction.ASC, _ID)) : pageable.getSort());

		if (isReplaceId)
		{
			var orders = result.getSort()
					.stream()
					.map(order -> Sort.by(order.getDirection(), idMatches.or(joinedIdMatches).test(order.getProperty())
							? replaceAllId.apply(order.getProperty())
							: order.getProperty()))
					.reduce(Sort::and)
					.orElseThrow(() -> new PrepareMongoPageableException(result.getSort().toString()));

			return PageRequest.of(result.getPageNumber(), result.getPageSize(), orders);
		}

		return result;
	}
}
