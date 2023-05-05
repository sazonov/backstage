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
import com.proit.app.exception.*;
import com.proit.app.model.dictitem.MongoDictDataItem;
import com.proit.app.service.backend.DictBackend;
import com.proit.app.service.backend.DictDataBackend;
import com.proit.app.service.mapping.mongo.MongoDictDataItemDictItemMapper;
import com.proit.app.service.query.MongoTranslator;
import com.proit.app.service.query.QueryParser;
import com.proit.app.service.query.ast.Empty;
import com.proit.app.service.query.ast.QueryExpression;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(name = "app.dicts.storage", havingValue = "mongoDB")
public class MongoDictDataBackend extends CommonMongoBackend implements DictDataBackend
{
	private static final String LOOKUP_SUFFIX = "@join";
	private static final Pattern ID_PATTERN = Pattern.compile("^(id)");
	private static final Pattern JOINED_ID_PATTERN = Pattern.compile("(\\.id)");

	private final DictBackend dictBackend;

	private final MongoDictDataItemDictItemMapper mongoDictDataItemDictItemMapper;

	private final QueryParser queryParser;
	private final MongoTranslator mongoTranslator;

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

		List<Object> result;

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

			result = mongoTemplate.find(query, Object.class, dictId);
		}
		else if (!fieldsToJoin.isEmpty())
		{
			var operations = new LinkedList<>(buildLookups(fieldsToJoin));
			operations.add(buildSort(sort, fieldsToJoin));

			if (pageable.isPaged())
			{
				operations.addAll(buildPaging(pageable));
			}

			result = mongoTemplate.aggregate(Aggregation.newAggregation(operations), dictId, Object.class)
					.getMappedResults();
		}
		else
		{
			try
			{
				var criteria = mongoTranslator.process(dictBackend.getDictById(dictId), queryExpression);

				result = mongoTemplate.find(query.addCriteria(criteria), Object.class, dictId);
			}
			catch (QuerySyntaxError e)
			{
				throw new QuerySyntaxError(e);
			}
		}

		var itemList = getPage(dictId, result, query, pageable);

		var isExistRefs = requiredFields.stream()
				.map(DictFieldName::getDictId)
				.anyMatch(Objects::nonNull);

		if (isExistRefs)
		{
			var requiredRefFields = requiredFields.stream()
					.filter(it -> it.getDictId() != null)
					.collect(Collectors.toList());

			buildRefsData(dictId, requiredRefFields, itemList);
		}

		return itemList.map(it -> buildMongoDictDataItem(dictId, it))
				.map(mongoDictDataItemDictItemMapper::map);
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

		return Stream.of(dictItem.getData())
				.peek(this::prepareDictDataMongoId)
				.map(data -> mongoTemplate.save(data, dictId))
				.map(data -> buildMongoDictDataItem(dictId, data))
				.map(mongoDictDataItemDictItemMapper::map)
				.findFirst()
				.orElseThrow(() -> {
					throw new DictionaryException("Ошибка при создании пользовательских данных справочника %s".formatted(dictId));
				});
	}

	//TODO: рассмотреть возможность замыкание на create
	@Override
	public List<DictItem> createMany(String dictId, List<DictItem> dictItems)
	{
		addToTransactionData(dictId, false);

		return dictItems.stream()
				.map(DictItem::getData)
				.peek(this::prepareDictDataMongoId)
				.map(data -> mongoTemplate.save(data, dictId))
				.map(data -> buildMongoDictDataItem(dictId, data))
				.map(mongoDictDataItemDictItemMapper::map)
				.toList();
	}

	@Override
	public DictItem update(String dictId, String itemId, long version, DictItem dictItem)
	{
		addToTransactionData(dictId, false);

		var query = Query.query(Criteria.where(_ID).is(itemId));

		prepareDictDataMongoId(dictItem.getData());

		update(dictId, version, dictItem.getData(), query);

		return mongoDictDataItemDictItemMapper.map(buildMongoDictDataItem(dictId, mongoTemplate.findOne(query, Object.class, dictId)));
	}

	@Override
	public void delete(String dictId, String itemId, boolean deleted, String reason)
	{
		addToTransactionData(dictId, false);

		var doc = new HashMap<String, Object>();

		if (deleted)
		{
			doc.put(DELETED, new Date());
			doc.put(DELETION_REASON, StringUtils.isBlank(reason) ? null : reason);
		}
		else
		{
			doc.put(DELETED, null);
		}

		var query = Query.query(Criteria.where(_ID).is(itemId));

		update(dictId, 0L, doc, query);
	}

	@Override
	public void deleteAll(String dictId, boolean deleted)
	{
		addToTransactionData(dictId, false);

		var doc = new HashMap<String, Object>();
		doc.put(DELETED, deleted ? new Date() : null);

//		TODO: зарефакторить, сделано в лоб
		var query = new Query(Criteria.where(DELETED).is(null));
		query.fields().include("_id");

		mongoTemplate.find(query, Map.class, dictId)
				.forEach(map -> update(dictId, 0L, doc, Query.query(Criteria.where(_ID).is(map.get(_ID)))));
	}

	@Override
	public long countByFilter(String dictId, QueryExpression queryExpression)
	{
		var criteria = mongoTranslator.process(dictBackend.getDictById(dictId), queryExpression);

		return mongoTemplate.count(new Query().addCriteria(criteria), dictId);
	}

	private void update(String dictId, long version, Map<String, Object> dictData, Query query)
	{
		var old = Optional.ofNullable(mongoTemplate.findOne(query, Map.class, dictId))
				.orElseThrow(NullPointerException::new);

		validatePessimisticLockForUpdate(version, dictData, old);

		var changes = getChanges(dictData, old);

		if (!changes.isEmpty())
		{
			mongoTemplate.upsert(query, buildUpdateClauses(old, changes), dictId);
		}
	}

	private Map<String, Object> getChanges(Map<String, Object> dictData, Map old)
	{
		return dictData.entrySet()
				.stream()
				.filter(it -> !it.getKey().equals(_ID))
				.filter(it -> (it.getValue() == null && old.get(it.getKey()) != null) || (it.getValue() != null && !it.getValue().equals(old.get(it.getKey()))))
				.collect(HashMap::new, (m, entry) -> m.put(entry.getKey(), entry.getValue()), HashMap::putAll);
	}

	private Update buildUpdateClauses(Map old, Map<String, Object> changes)
	{
		var updateClauses = new Update();

		var updateDate = new Date();
		updateClauses.set(UPDATED, updateDate);
		changes.put(UPDATED, updateDate);

		updateClauses.set(VERSION, (Long) old.get(VERSION) + 1);
		changes.put(VERSION, (Long) old.get(VERSION) + 1);

		changes.forEach(updateClauses::set);

		var history = (List) old.get(HISTORY);
		history.add(changes);

		updateClauses.set(HISTORY, history);

		return updateClauses;
	}

	private void validatePessimisticLockForUpdate(long version, Map<String, Object> mappedDoc, Map old)
	{
		if (version != (Long) old.get(VERSION) && (!mappedDoc.containsKey(DELETED) || mappedDoc.size() > 1)
				&& (!mappedDoc.containsKey(DELETED) && !mappedDoc.containsKey(DELETION_REASON) || mappedDoc.size() > 2))
		{
			throw new DictionaryConcurrentUpdateException(version, (Long) old.get(VERSION));
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

	private Page<Object> getPage(String dictId, List<Object> dictItems, Query query, Pageable pageable)
	{
		return PageableExecutionUtils.getPage(
				dictItems,
				pageable,
				() -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), dictId));
	}

	private void buildRefsData(String dictId, List<DictFieldName> requiredFields, Page<Object> itemList)
	{
		var refFieldMap = dictBackend.getDictById(dictId)
				.getFields()
				.stream()
				.filter(it -> it.getType() == DictFieldType.DICT)
				.collect(Collectors.toMap(Function.identity(), it -> getRefIds(it.getId(), itemList)));

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

			var refItems = mongoTemplate.find(refQuery, Object.class, dictField.getDictRef().getDictId())
					.stream()
					.map(o -> (Map<String, Object>) o)
					.collect(Collectors.toMap(map -> map.get(_ID) instanceof String s ? s : map.get(_ID).toString(), Function.identity()));

			matchRefsData(itemList, dictField.getId(), refItems);
		}));
	}

	private void matchRefsData(Page<Object> itemList, String fieldId, Map<String, Map<String, Object>> refItems)
	{
		itemList.stream()
				.map(o -> (Map<String, Object>) o)
				.filter(map -> map.get(fieldId) != null)
				.forEach(map -> {
					if (map.get(fieldId) instanceof List list)
					{
						list = list.stream()
								.map(it -> refItems.get((String) map.get((String) it)))
								.toList();

						map.put(fieldId, list);
						return;
					}

					map.put(fieldId, refItems.get((String) map.get(fieldId)));
				});
	}

	private List<String> getRefIds(String fieldId, Page<Object> items)
	{
		return items.stream()
				.map(o -> (Map<String, Object>) o)
				.flatMap(map -> {
					if (map.get(fieldId) instanceof List list)
					{
						return list.stream();
					}
					return Stream.of((String) map.get(fieldId));
				})
				.distinct()
				.toList();
	}

	private MongoDictDataItem buildMongoDictDataItem(String dictId, Object data)
	{
		return MongoDictDataItem.of(dictId, data);
	}

	private void prepareDictDataMongoId(Map<String, Object> dictData)
	{
		if (dictData.containsKey(ID))
		{
			dictData.put(_ID, dictData.get(ID));
			dictData.remove(ID);
		}
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
