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

import com.mongodb.BasicDBObject;
import com.mongodb.MongoNamespace;
import com.proit.app.domain.*;
import com.proit.app.exception.dict.DictAlreadyExistsException;
import com.proit.app.exception.dict.DictNotFoundException;
import com.proit.app.exception.dict.enums.EnumNotFoundException;
import com.proit.app.service.backend.DictSchemeBackend;
import com.proit.app.service.backend.Engine;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

import static com.proit.app.constant.ServiceFieldConstants.ID;
import static com.proit.app.constant.ServiceFieldConstants._ID;
import static java.util.function.Predicate.not;
import static org.springframework.data.mongodb.core.schema.JsonSchemaProperty.*;

@Component
@RequiredArgsConstructor
public class MongoDictSchemeBackend extends AbstractMongoBackend implements DictSchemeBackend
{
	@Override
	public Engine getEngine()
	{
		return mongoEngine;
	}

	@Override
	public Dict createDictScheme(Dict dict)
	{
		var id = dict.getId();

		if (existsDictSchemeById(id))
		{
			throw new DictAlreadyExistsException(id);
		}

		addTransactionData(id, true);

		addMongoServiceFields(dict.getFields());

		mongoTemplate.createCollection(id, buildCollectionOptions(dict));

//		TODO: Валидация индексов при создании
		dict.getIndexes()
				.forEach(it -> mongoTemplate.indexOps(id).ensureIndex(buildIndex(it)));

		convertMongoServiceFields(dict);

		return dict;
	}

	@Override
	public Dict updateDictScheme(String dictId, Dict updatedDict)
	{
		addTransactionData(dictId, true);

		addMongoServiceFields(updatedDict.getFields());

		if (!StringUtils.equals(dictId, updatedDict.getId()))
		{
			renameDictSchemeById(dictId, updatedDict.getId());
		}

		var params = new LinkedHashMap<String, Object>();
		params.put("collMod", updatedDict.getId());
		params.put("validator", buildMongoJsonSchema(updatedDict).toDocument());

		mongoTemplate.executeCommand(new Document(params));

		convertMongoServiceFields(updatedDict);

		return updatedDict;
	}

	@Override
	public void renameDictSchemeById(String dictId, String renamedDictId)
	{
		if (!existsDictSchemeById(dictId))
		{
			throw new DictNotFoundException(dictId);
		}

		mongoTemplate.getCollection(dictId)
				.renameCollection(new MongoNamespace(mongoTemplate.getDb().getName(), renamedDictId));
	}

	@Override
	public void deleteDictSchemeById(String dictId)
	{
		if (!existsDictSchemeById(dictId))
		{
			throw new DictNotFoundException(dictId);
		}

		mongoTemplate.dropCollection(dictId);
	}

	@Override
	public boolean existsDictSchemeById(String dictId)
	{
		return mongoTemplate.collectionExists(dictId);
	}

	@Override
	public DictField renameDictField(Dict dict, String oldFieldId, DictField field)
	{
		addTransactionData(dict.getId(), true);

		if (!oldFieldId.equals(field.getId()))
		{
			var updateQuery = new BasicDBObject();
			updateQuery.append("$rename", new BasicDBObject().append(oldFieldId, field.getId()));
			mongoTemplate.getCollection(dict.getId()).updateMany(new BasicDBObject(), updateQuery);
		}

		return field;
	}

	@Override
	public DictConstraint createConstraint(Dict dict, DictConstraint constraint)
	{
		addTransactionData(dict.getId(), true);

		mongoTemplate.indexOps(dict.getId()).ensureIndex(buildIndex(constraint));

		return constraint;
	}

	@Override
	public void deleteConstraint(Dict dict, String id)
	{
		addTransactionData(dict.getId(), true);

		mongoTemplate.indexOps(dict.getId()).dropIndex(id);
	}

	@Override
	public DictIndex createIndex(Dict dict, DictIndex index)
	{
		addTransactionData(dict.getId(), true);

		mongoTemplate.indexOps(dict.getId()).ensureIndex(buildIndex(index));

		return index;
	}

	@Override
	public void deleteIndex(Dict dict, String id)
	{
		addTransactionData(dict.getId(), true);

		mongoTemplate.indexOps(dict.getId()).dropIndex(id);
	}

	private CollectionOptions buildCollectionOptions(Dict dict)
	{
		return CollectionOptions.empty()
				.schema(buildMongoJsonSchema(dict));
	}

	private Index buildIndex(DictConstraint source)
	{
		var target = new Index().named(source.getId());

		source.getFields().forEach(it -> target.on(it, Sort.Direction.ASC));

		target.unique();

		return target;
	}

	//TODO: провести рефакторинг
	private MongoJsonSchema buildMongoJsonSchema(Dict dict)
	{
		var builder = MongoJsonSchema.builder();

		dict.getFields()
				.stream()
				.filter(DictField::isRequired)
				.filter(not(it -> it.getType() == DictFieldType.ENUM || it.getType() == DictFieldType.JSON))
				.forEach(it -> {
					var property = getPropertyByDictField(it.getType(), it.getId());

					builder.property(required(it.isMultivalued()
							? array(it.getId()).items(property)
							: property));
				});

		dict.getFields()
				.stream()
				.filter(DictField::isRequired)
				.filter(it -> it.getType() == DictFieldType.ENUM)
				.forEach(it -> {
					var property = dict.getEnums()
							.stream()
							.filter(e -> e.getId().equals(it.getEnumId()))
							.findFirst()
							.map(e -> string(it.getId()).possibleValues(e.getValues()))
							.orElseThrow(() -> new EnumNotFoundException(it.getEnumId()));

					builder.property(required(it.isMultivalued()
							? array(it.getId()).items(property)
							: property));
				});

		return builder.build();
	}

	private JsonSchemaProperty getPropertyByDictField(DictFieldType type, String fieldId)
	{
		return switch (type)
				{
					case INTEGER -> int64(fieldId);
					case DECIMAL -> decimal128(fieldId);
					case STRING, DICT, ATTACHMENT, GEO_JSON -> string(fieldId);
					case BOOLEAN -> named(fieldId).ofType(new Type.JsonType("boolean"));
					case DATE, TIMESTAMP -> date(fieldId);

					default -> throw new RuntimeException("unsupported type: %s".formatted(type));
				};
	}

	//TODO: подумать над тем, как сделать это обязательным контрактом
	// чтобы при реализации дополнительного адаптера, характерные сервисные поля были добавлены, а не пропущены.
	private void addMongoServiceFields(List<DictField> dictFields)
	{
		dictFields.stream()
				.filter(it -> it.getId().equals(ID))
				.forEach(it -> it.setId(_ID));
	}
}
