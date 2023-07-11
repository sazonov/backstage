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
import com.proit.app.configuration.properties.DictsProperties;
import com.proit.app.domain.*;
import com.proit.app.exception.dictionary.DictAlreadyExistsException;
import com.proit.app.exception.dictionary.DictDeletedException;
import com.proit.app.exception.dictionary.DictNotFoundException;
import com.proit.app.exception.dictionary.enums.EnumNotFoundException;
import com.proit.app.service.backend.DictBackend;
import com.proit.app.service.backend.Storage;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.schema.JsonSchemaObject.Type.JsonType;
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.proit.app.constant.ServiceFieldConstants.ID;
import static com.proit.app.constant.ServiceFieldConstants._ID;
import static java.util.function.Predicate.not;
import static org.springframework.data.mongodb.core.schema.JsonSchemaProperty.*;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = DictsProperties.STORAGE_PROPERTY, havingValue = MongoStorage.MONGO_STORAGE)
public class MongoDictBackend extends CommonMongoBackend implements DictBackend
{
	private final MongoStorage mongoStorage;

	@Override
	public Storage getStorage()
	{
		return mongoStorage;
	}

	@Override
	public Dict getDictById(String id)
	{
		var dict = mongoDictRepository.findById(id)
				.orElseThrow(() -> new DictNotFoundException(id));

		if (dict.getDeleted() != null)
		{
			throw new DictDeletedException(id);
		}

		convertMongoServiceFields(dict);

		return dict;
	}

	@Override
	public List<Dict> getAllDicts()
	{
		var result = mongoDictRepository.findAll();

		result.forEach(this::convertMongoServiceFields);

		return result;
	}

	@Override
	public Dict createDict(Dict dict)
	{
		addToTransactionData(dict.getId(), true);

		var id = dict.getId();

		if (mongoDictRepository.existsById(id))
		{
			throw new DictAlreadyExistsException(id);
		}

		addMongoServiceFields(dict.getFields());

		mongoTemplate.createCollection(id, buildCollectionOptions(dict));

//		TODO: Валидация индексов при создании
		dict.getIndexes()
				.forEach(it -> mongoTemplate.indexOps(id).ensureIndex(buildIndex(it)));

		var result = mongoDictRepository.save(dict);

		convertMongoServiceFields(result);

		return result;
	}

	@Override
	public Dict updateDict(String dictId, Dict updatedDict)
	{
		addToTransactionData(dictId, true);

		addMongoServiceFields(updatedDict.getFields());

		if (!dictId.equals(updatedDict.getId()))
		{
			mongoTemplate.getCollection(dictId)
					.renameCollection(new MongoNamespace(mongoTemplate.getDb().getName(), updatedDict.getId()));

			mongoDictRepository.deleteById(dictId);
		}

		var params = new LinkedHashMap<String, Object>();
		params.put("collMod", updatedDict.getId());
		params.put("validator", buildMongoJsonSchema(updatedDict).toDocument());

		mongoTemplate.executeCommand(new Document(params));

		var result = mongoDictRepository.save(updatedDict);

		convertMongoServiceFields(result);

		return result;
	}

	@Override
	//	TODO: История изменений схемы, даты создания/обновления схемы?
	public void deleteDict(String id, LocalDateTime deleted)
	{
		addToTransactionData(id, true);

		var scheme = mongoDictRepository.findById(id)
				.orElseThrow(() -> new DictNotFoundException(id));

		scheme.setDeleted(deleted);

		mongoDictRepository.save(scheme);
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

		mongoDictRepository.save(dict);

		return field;
	}

	@Override
	public DictConstraint createConstraint(Dict dict, DictConstraint constraint)
	{
		addToTransactionData(dict.getId(), true);

		mongoTemplate.indexOps(dict.getId()).ensureIndex(buildIndex(constraint));

		dict.getConstraints().add(constraint);

		mongoDictRepository.save(dict);

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

		mongoDictRepository.save(dict);
	}

	@Override
	public DictIndex createIndex(Dict dict, DictIndex index)
	{
		addToTransactionData(dict.getId(), true);

		mongoTemplate.indexOps(dict.getId()).ensureIndex(buildIndex(index));

		dict.getIndexes().add(index);

		mongoDictRepository.save(dict);

		return index;
	}

	@Override
	public void deleteIndex(Dict dict, String id)
	{
		addToTransactionData(dict.getId(), true);

		mongoTemplate.indexOps(dict.getId()).dropIndex(id);

		dict.setIndexes(dict.getIndexes()
				.stream()
				.filter(it -> !it.getId().equals(id))
				.collect(Collectors.toList()));

		mongoDictRepository.save(dict);
	}

	@Override
	public DictEnum createEnum(Dict dict, DictEnum dictEnum)
	{
		addToTransactionData(null, true);

		dict.getEnums().add(dictEnum);
		mongoDictRepository.save(dict);

		return dictEnum;
	}

	@Override
	public DictEnum updateEnum(Dict dict, DictEnum oldEnum, DictEnum newEnum)
	{
		addToTransactionData(null, true);

		oldEnum.setValues(newEnum.getValues());
		oldEnum.setName(newEnum.getName());

		mongoDictRepository.save(dict);

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

		mongoDictRepository.save(dict);
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
					case STRING, DICT, ATTACHMENT, GEO_JSON -> string(fieldId);
					case BOOLEAN -> named(fieldId).ofType(new JsonType("boolean"));
					case DATE, TIMESTAMP -> date(fieldId);

					default -> throw new RuntimeException("unsupported type: %s".formatted(type));
				};
	}

	//TODO: подумать над тем, как сделать это обязательным контрактом
	// чтобы при реализации дополнительного адаптера, характерные сервисные поля были добавлены, а не пропущены.
	private void addMongoServiceFields(List<DictField> dictFields)
	{
		dictFields.add(0, DictField.builder()
				.id(_ID)
				.name("Идентификатор")
				.type(DictFieldType.STRING)
				.required(false)
				.multivalued(false)
				.build());
	}

	//TODO: рассмотреть возможность перемещение конвертации в эдвайс монго бэкенда
	private void convertMongoServiceFields(Dict dict)
	{
		dict.getFields()
				.stream()
				.filter(it -> it.getId().equalsIgnoreCase(_ID))
				.forEach(it -> it.setId(ID));
	}
}
