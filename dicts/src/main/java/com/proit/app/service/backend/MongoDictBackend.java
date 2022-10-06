/*
 *    Copyright 2019-2022 the original author or authors.
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

package com.proit.app.service.backend;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoNamespace;
import com.proit.app.domain.*;
import com.proit.app.exception.*;
import com.proit.app.repository.DictRepository;
import com.proit.app.service.query.QueryParser;
import com.proit.app.service.query.Translator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.OutOperation;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Component;

import javax.persistence.RollbackException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.proit.app.constant.ServiceFieldConstants.*;
import static java.util.function.Predicate.not;
import static org.springframework.data.mongodb.core.schema.JsonSchemaProperty.*;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.dicts.storage", havingValue = "mongoDB")
public class MongoDictBackend implements DictBackend
{
	public static final String SCHEME_COLLECTION_ID = "dict";
	// TODO: это уже не нужно.
	public static final String ENUM_COLLECTION_ID = "dictEnum";

	private final MongoTemplate mongoTemplate;

	private final DictRepository dictRepository;

	private final Translator translator;
	private final QueryParser queryParser;

	private final AtomicBoolean activeTransaction = new AtomicBoolean(false);
	private TransactionData transactionData;

	@Getter
	public static class TransactionData
	{
		private final Map<String, String> affectedDictIds = new LinkedHashMap<>();
	}

	@Override
	public void beginDDL()
	{
		if (activeTransaction.get())
		{
			throw new DictionaryException("Транзакция DDL уже открыта.");
		}

		if (activeTransaction.compareAndSet(false, true))
		{
			transactionData = new TransactionData();
		}
		else
		{
			throw new DictionaryException("Транзакция DDL уже открыта.");
		}
	}

	@Override
	public void commitDDL()
	{
		if (!activeTransaction.get())
		{
			throw new DictionaryException("Отсутствует транзакция DDL.");
		}

		transactionData.getAffectedDictIds().forEach((original, copied) -> mongoTemplate.dropCollection(copied));

		transactionData = null;
		activeTransaction.set(false);
	}

	@Override
	public void rollbackDDL(Exception e)
	{
		if (!activeTransaction.get())
		{
			throw new DictionaryException("Отсутствует транзакция DDL.");
		}

		transactionData.getAffectedDictIds().forEach((original, copied) ->
		{
			mongoTemplate.dropCollection(original);

			if (original.equals(SCHEME_COLLECTION_ID) || original.equals(ENUM_COLLECTION_ID))
			{
				mongoTemplate.getCollection(copied)
						.renameCollection(new MongoNamespace(mongoTemplate.getDb().getName(), original));
			}
			else if (dictRepository.existsById(original))
			{
				mongoTemplate.getCollection(copied)
						.renameCollection(new MongoNamespace(mongoTemplate.getDb().getName(), original));

				dictRepository.findById(original)
						.orElseThrow(() -> new DictionaryNotFoundException(original))
						.getIndexes()
						.forEach(it -> mongoTemplate.indexOps(original).ensureIndex(buildIndex(it)));
			}
			else
			{
				mongoTemplate.dropCollection(copied);
			}
		});

		transactionData = null;
		activeTransaction.set(false);

		throw new RollbackException(e);
	}

	public Object getById(String dictId, String id, List<DictFieldName> requiredFields)
	{
		var query = "%s = '%s'".formatted(_ID, id);

		return getByFilter(getDictById(dictId), requiredFields, query, Pageable.unpaged())
				.getContent()
				.stream()
				.findFirst()
				.orElseThrow(() -> new ObjectNotFoundException(Dict.class, dictId));
	}

	@Override
	public List<Object> getByIds(String dictId, List<String> ids, List<DictFieldName> requiredFields)
	{
		var filtersQuery = "%s in (%s)".formatted(_ID, String.join(",", ids.stream().map(it -> "'" + it + "'").toList()));

		return getByFilter(getDictById(dictId), requiredFields, filtersQuery, Pageable.unpaged()).getContent();
	}

	@Override
	public Page<Object> getByFilter(Dict dict, List<DictFieldName> requiredFields, String filtersQuery, Pageable pageable)
	{
		var requiredBasicFields = requiredFields.stream()
				.filter(it -> it.getDictId() == null)
				.map(DictFieldName::getFieldId)
				.collect(Collectors.toList());

		List<Object> result;

		var query = new Query().with(pageable);
		query.collation(Collation.of("ru").strength(Collation.ComparisonLevel.secondary()));

		if (!requiredBasicFields.contains("*"))
		{
			var fields = query.fields();

			requiredBasicFields.forEach(fields::include);
		}

		if (filtersQuery == null)
		{
			requiredBasicFields.remove("*");

			result = mongoTemplate.find(query, Object.class, dict.getId());
		}
		else
		{
			try
			{
				var criteria = translator.process(dict, queryParser.parse(filtersQuery.replaceAll("^(id)", _ID).replaceAll("([^\\S]+(id))", " " + _ID)));
				result = mongoTemplate.find(query.addCriteria(criteria), Object.class, dict.getId());
			}
			catch (QuerySyntaxError e)
			{
				throw new RuntimeException(e);
			}
		}

		var itemList = getPage(dict.getId(), result, query, pageable);

		var isExistRefs = requiredFields.stream()
				.map(DictFieldName::getDictId)
				.anyMatch(Objects::nonNull);

		if (isExistRefs)
		{
			var requiredRefFields = requiredFields.stream()
					.filter(it -> it.getDictId() != null)
					.collect(Collectors.toList());

			buildRefsData(dict.getId(), requiredRefFields, itemList);
		}

		return itemList;
	}

	@Override
	public boolean existsById(String dictId, String itemId)
	{
		var query = Query.query(Criteria.where(_ID).is(itemId));

		return mongoTemplate.exists(query, dictId);
	}

	@Override
	public boolean existsByFilter(String dictId, String filtersQuery)
	{
		var criteria = translator.process(getDictById(dictId), queryParser.parse(filtersQuery));

		return mongoTemplate.exists(new Query().addCriteria(criteria), dictId);
	}

	@Override
	public long countByFilter(String dictId, String filtersQuery)
	{
		var criteria = translator.process(getDictById(dictId), queryParser.parse(filtersQuery));

		return mongoTemplate.count(new Query().addCriteria(criteria), dictId);
	}

	@Override
	public Map<String, Object> create(String dictId, Map<String, Object> mappedDoc)
	{
		addToTransactionData(dictId, false);

		if (mappedDoc.containsKey(ID))
		{
			mappedDoc.put(_ID, mappedDoc.get(ID));
			mappedDoc.remove(ID);
		}

		buildServiceFields(mappedDoc);

		return mongoTemplate.save(mappedDoc, dictId);
	}

	@Override
	public List<Map<String, Object>> createMany(String dictId, List<Map<String, Object>> docs)
	{
		addToTransactionData(dictId, false);

		return docs.stream()
				.peek(doc -> {
					if (doc.containsKey(ID))
					{
						doc.put(_ID, doc.get(ID));
						doc.remove(ID);
					}
				})
				.map(this::buildServiceFields)
				.map(it -> mongoTemplate.save(it, dictId))
				.toList();
	}

	@Override
	public Object update(String dictId, String itemId, long version, Map<String, Object> mappedDoc)
	{
		addToTransactionData(dictId, false);

		var query = Query.query(Criteria.where(_ID).is(itemId));

		validatedUpdate(dictId, version, mappedDoc, query);

		return mongoTemplate.findOne(query, Map.class, dictId);
	}

	@Override
	public void delete(String dictId, String itemId, boolean deleted)
	{
		addToTransactionData(dictId, false);

		var doc = new HashMap<String, Object>();
		doc.put(DELETED, deleted ? new Date() : null);

		var query = Query.query(Criteria.where(_ID).is(itemId));
		validatedUpdate(dictId, 0L, doc, query);
	}

	@Override
	public Dict getDictById(String id)
	{
		return dictRepository.findById(id)
				.map(it -> {
					if (it.getDeleted() != null)
					{
						throw new DictionaryDeletedException(it.getId());
					}
					return it;
				})
				.orElseThrow(() -> new DictionaryNotFoundException(id));
	}

	@Override
	public List<Dict> getAllDicts()
	{
		return dictRepository.findAll();
	}

	@Override
	public Dict createDict(Dict dict)
	{
		addToTransactionData(dict.getId(), true);

		var id = dict.getId();

		if (dictRepository.existsById(id))
		{
			throw new DictionaryAlreadyExistsException(id);
		}

		mongoTemplate.createCollection(id, buildCollectionOptions(dict));

//		TODO: Валидация индексов при создании
		dict.getIndexes()
				.forEach(it -> mongoTemplate.indexOps(id).ensureIndex(buildIndex(it)));

		return dictRepository.save(dict);
	}

	@Override
	//	TODO: История изменений схемы, даты создания/обновления схемы?
	public void deleteDict(String id, LocalDateTime deleted)
	{
		addToTransactionData(id, true);

		var scheme = dictRepository.findById(id)
				.orElseThrow(() -> new DictionaryNotFoundException(id));

		scheme.setDeleted(deleted);

		dictRepository.save(scheme);
	}

	@Override
	public Dict updateDict(String dictId, Dict updatedDict)
	{
		addToTransactionData(dictId, true);

		if (!dictId.equals(updatedDict.getId()))
		{
			mongoTemplate.getCollection(dictId)
					.renameCollection(new MongoNamespace(mongoTemplate.getDb().getName(), updatedDict.getId()));

			dictRepository.deleteById(dictId);
		}

		var params = new LinkedHashMap<String, Object>();
		params.put("collMod", updatedDict.getId());
		params.put("validator", buildMongoJsonSchema(updatedDict).toDocument());

		mongoTemplate.executeCommand(new Document(params));

		return dictRepository.save(updatedDict);
	}

	@Override
	public DictField renameDictField(Dict dict, String oldFieldId, DictField field)
	{
		addToTransactionData(dict.getId(), true);

		if (!oldFieldId.equals(field.getId()))
		{
			var updateQuery = new BasicDBObject();
			updateQuery.append("$rename", new BasicDBObject().append(oldFieldId, field.getId()));
			mongoTemplate.getCollection(dict.getId()).updateMany(new BasicDBObject(), updateQuery);
		}

		dictRepository.save(dict);

		return field;
	}

	@Override
	public DictConstraint createConstraint(Dict dict, DictConstraint constraint)
	{
		addToTransactionData(dict.getId(), true);

		mongoTemplate.indexOps(dict.getId()).ensureIndex(buildIndex(constraint));

		dict.getConstraints().add(constraint);

		dictRepository.save(dict);

		return constraint;
	}

	@Override
	public void deleteConstraint(Dict dict, String id)
	{
		addToTransactionData(dict.getId(), true);

		mongoTemplate.indexOps(dict.getId()).dropIndex(id);

		dict.setConstraints(dict.getConstraints().stream()
				.filter(it -> !it.getId().equals(id))
				.collect(Collectors.toList()));

		dictRepository.save(dict);
	}

	@Override
	public DictIndex createIndex(Dict dict, DictIndex index)
	{
		addToTransactionData(dict.getId(), true);

		mongoTemplate.indexOps(dict.getId()).ensureIndex(buildIndex(index));

		dict.getIndexes().add(index);

		dictRepository.save(dict);

		return index;
	}

	@Override
	public void deleteIndex(Dict dict, String id)
	{
		addToTransactionData(dict.getId(), true);

		mongoTemplate.indexOps(dict.getId()).dropIndex(id);

		dict.setIndexes(dict.getIndexes().stream()
				.filter(it -> !it.getId().equals(id))
				.collect(Collectors.toList()));

		dictRepository.save(dict);
	}

	@Override
	public DictEnum createEnum(Dict dict, DictEnum dictEnum)
	{
		addToTransactionData(null, true);

		dict.getEnums().add(dictEnum);
		dictRepository.save(dict);

		return dictEnum;
	}

	@Override
	public DictEnum updateEnum(Dict dict, DictEnum oldEnum, DictEnum newEnum)
	{
		addToTransactionData(null, true);

		oldEnum.setValues(newEnum.getValues());
		oldEnum.setName(newEnum.getName());

		dictRepository.save(dict);

		return newEnum;
	}

	@Override
	public void deleteEnum(Dict dict, String enumId)
	{
		addToTransactionData(null, true);

		dict.setEnums(
				dict.getEnums()
						.stream()
						.filter(it -> !it.getId().equals(enumId))
						.toList()
		);

		dictRepository.save(dict);
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
				.forEach(map -> validatedUpdate(dictId, 0L, doc, Query.query(Criteria.where(_ID).is(map.get(_ID)))));

	}

	private Map<String, Object> buildServiceFields(Map<String, Object> doc)
	{
		doc.put(CREATED, new Date());
		doc.put(UPDATED, new Date());
		doc.put(DELETED, null);
		doc.put(VERSION, 1L);
		doc.put(HISTORY, List.of(new HashMap<>(doc)));

		return doc;
	}

	private void validatedUpdate(String dictId, long version, Map<String, Object> mappedDoc, Query query)
	{
		var old = Optional.ofNullable(mongoTemplate.findOne(query, Map.class, dictId))
				.orElseThrow(NullPointerException::new);

		if (version != (Long) old.get(VERSION) && (!mappedDoc.containsKey(DELETED) || mappedDoc.size() > 1))
		{
			throw new DictionaryConcurrentUpdateException(version, (Long) old.get(VERSION));
		}

		var update = new Update();

		var changes = mappedDoc.entrySet()
				.stream()
				.filter(it -> !it.getKey().equals(_ID))
				.filter(it -> (it.getValue() == null && old.get(it.getKey()) != null) || (it.getValue() != null && !it.getValue().equals(old.get(it.getKey()))))
				.peek(it -> update.set(it.getKey(), it.getValue()))
				.collect(HashMap::new, (m, entry) -> m.put(entry.getKey(), entry.getValue()), HashMap::putAll);

		if (changes.size() != 0)
		{
			var updateDate = new Date();
			update.set(UPDATED, updateDate);
			changes.put(UPDATED, updateDate);

			update.set(VERSION, (Long) old.get(VERSION) + 1);
			changes.put(VERSION, (Long) old.get(VERSION) + 1);

			var history = (List) old.get(HISTORY);
			history.add(changes);

			update.set(HISTORY, history);

			mongoTemplate.upsert(query, update, dictId);
		}
	}

	private void buildRefsData(String dictId, List<DictFieldName> requiredFields, Page<Object> itemList)
	{
		var refFieldMap = dictRepository.findById(dictId)
				.orElseThrow(() -> new DictionaryNotFoundException(dictId))
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

	private Page<Object> getPage(String dictId, List<Object> list, Query query, Pageable pageable)
	{
		return PageableExecutionUtils.getPage(
				list,
				pageable,
				() -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), dictId));
	}

	private CollectionOptions buildCollectionOptions(Dict dict)
	{
		return CollectionOptions.empty()
				.schema(buildMongoJsonSchema(dict));
	}

	private Index buildIndex(DictIndex source)
	{
		var target = new Index().named(source.getId());

		source.getFields().forEach(it -> target.on(it, source.getDirection()));

		return target;
	}

	private Index buildIndex(DictConstraint source)
	{
		var target = new Index().named(source.getId());

		source.getFields().forEach(it -> target.on(it, Sort.Direction.ASC));

		target.unique();

		return target;
	}

	private MongoJsonSchema buildMongoJsonSchema(Dict dict)
	{
		var builder = MongoJsonSchema.builder();

		dict.getFields().stream().filter(DictField::isRequired).filter(not(it -> it.getType() == DictFieldType.ENUM || it.getType() == DictFieldType.JSON)).forEach(it -> {
			var property = getPropertyByDictField(it.getType(), it.getId());

			builder.property(required(it.isMultivalued() ? array(it.getId()).items(property) : property));
		});

		dict.getFields().stream().filter(DictField::isRequired).filter(it -> it.getType() == DictFieldType.ENUM).forEach(it -> {
			var property = dict.getEnums()
					.stream()
					.filter(e -> e.getId().equals(it.getEnumId()))
					.findFirst()
					.map(e -> string(it.getId()).possibleValues(e.getValues()))
					.orElseThrow(() -> new EnumNotFoundException(it.getEnumId()));

			builder.property(required(it.isMultivalued() ? array(it.getId()).items(property) : property));
		});

		return builder.build();
	}

	private JsonSchemaProperty getPropertyByDictField(DictFieldType type, String fieldId)
	{
		return switch (type)
				{
					case INTEGER -> int64(fieldId);
					case DECIMAL -> decimal128(fieldId);
					case STRING, DICT, ATTACHMENT -> string(fieldId);
					case BOOLEAN -> bool(fieldId);
					case DATE, TIMESTAMP -> date(fieldId);

					default -> throw new RuntimeException();
				};
	}

	private void addToTransactionData(String dictId, boolean schemeUsed)
	{
		if (activeTransaction.get())
		{
			if (schemeUsed && !transactionData.getAffectedDictIds().containsKey(SCHEME_COLLECTION_ID))
			{
				var dictCopyId = copyDict(SCHEME_COLLECTION_ID);
				transactionData.getAffectedDictIds().put(SCHEME_COLLECTION_ID, dictCopyId);
			}

			if (dictId != null && !transactionData.getAffectedDictIds().containsKey(dictId))
			{
				var dictCopyId = copyDict(dictId);
				transactionData.getAffectedDictIds().put(dictId, dictCopyId);
			}
		}
	}

	private String copyDict(String dictId)
	{
		var copyDictId = dictId + "_clone";
		mongoTemplate.createCollection(copyDictId);

		var outOperation = new OutOperation(copyDictId);
		mongoTemplate.aggregate(Aggregation.newAggregation(outOperation), dictId, BasicDBObject.class);

		return copyDictId;
	}
}
