package com.proit.app.common;

import com.proit.app.configuration.db.MongoConfiguration;
import com.proit.app.domain.*;
import com.proit.app.exception.EnumNotFoundException;
import com.proit.app.repository.DictRepository;
import org.bson.types.Decimal128;
import org.junit.ClassRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.schema.JsonSchemaObject.Type.JsonType;
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;

import static com.proit.app.constant.ServiceFieldConstants.*;
import static java.util.function.Predicate.not;
import static org.springframework.data.mongodb.core.schema.JsonSchemaProperty.*;

@SpringBootTest
@ContextConfiguration(initializers = {AbstractTest.Initializer.class})
@Import({JacksonAutoConfiguration.class, MongoConfiguration.class})
public class AbstractTest
{
	public static final String DICT_ID = "test";
	public static final String DELETED_DICT_ID = "deletedDict";
	public static final String REF_DICT_ID = "refDict";
	public static final String DICT_ATTACHMENT_ID = "test_attachment";

	public static final String INDEX_ID = "testIndex";

	public static final Dict dict = Dict.builder()
			.id(DICT_ID)
			.name("Тестовая коллекция")
			.build();

	public static final Dict refDict = Dict.builder()
			.id(REF_DICT_ID)
			.name("Ссылающаяся коллекция")
			.build();

	public static final Dict deletedDict = Dict.builder()
			.id(DELETED_DICT_ID)
			.name("Удаленная коллекция")
			.deleted(LocalDateTime.now())
			.build();

	public static final Dict dictAttachment = Dict.builder()
			.id(DICT_ATTACHMENT_ID)
			.name("Тестовая коллекция с вложением")
			.build();

	@Autowired
	private DictRepository dictRepository;

	@Autowired
	private MongoTemplate mongoTemplate;

	@ClassRule
	static final MongoDBContainer mongo;

	@ClassRule
	static final PostgreSQLContainer<?> postgres;

	static
	{
		mongo = new MongoDBContainer("mongo:5");
		postgres = new PostgreSQLContainer<>("postgres");

		mongo.start();
		postgres.start();
	}

	static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext>
	{
		@Override
		public void initialize(ConfigurableApplicationContext configurableApplicationContext)
		{
			TestPropertyValues.of(
					"app.mongo.host=" + mongo.getContainerIpAddress(),
					"app.mongo.port=" + mongo.getMappedPort(27017),
					"app.mongo.database=" + "dicts",
					"app.dataSource.driverClassName=" + postgres.getDriverClassName(),
					"app.dataSource.url=" + postgres.getJdbcUrl(),
					"app.dataSource.username=" + postgres.getUsername(),
					"app.dataSource.password=" + postgres.getPassword()
			).applyTo(configurableApplicationContext.getEnvironment());
		}
	}

	@PostConstruct
	void init()
	{
		if (dictRepository.findAll().stream().map(Dict::getId).noneMatch(it -> Set.of(DICT_ID, DELETED_DICT_ID, REF_DICT_ID).contains(it)))
		{
			var fields = new ArrayList<DictField>();
			addServiceFields(fields);

			fields.add(
					DictField.builder()
							.id("stringField")
							.name("строка")
							.type(DictFieldType.STRING)
							.required(true)
							.multivalued(false)
							.build()
			);

			fields.add(
					DictField.builder()
							.id("integerField")
							.name("число")
							.type(DictFieldType.INTEGER)
							.required(true)
							.multivalued(false)
							.build()
			);

			fields.add(
					DictField.builder()
							.id("doubleField")
							.name("вещественное число")
							.type(DictFieldType.DECIMAL)
							.required(true)
							.multivalued(false)
							.build()
			);

			fields.add(
					DictField.builder()
							.id("timestampField")
							.name("Дата и время")
							.type(DictFieldType.TIMESTAMP)
							.required(false)
							.multivalued(true)
							.build()
			);

			fields.add(
					DictField.builder()
							.id("booleanField")
							.name("Булево")
							.type(DictFieldType.BOOLEAN)
							.required(true)
							.multivalued(false)
							.build()
			);

			dict.setFields(fields);

			var index = DictIndex.builder()
					.id(INDEX_ID)
					.fields(List.of("stringField"))
					.build();

			dict.setIndexes(List.of(index));

			mongoTemplate.createCollection(dict.getId(), buildCollectionOptions(dict));
			mongoTemplate.indexOps(dict.getId())
					.ensureIndex(new Index(index.getFields().get(0), Sort.Direction.ASC).named(index.getId()));
			dictRepository.save(dict);

			deletedDict.setFields(fields);

			mongoTemplate.createCollection(deletedDict.getId(), buildCollectionOptions(deletedDict));
			dictRepository.save(deletedDict);

			var dictAttachmentFields = new ArrayList<>(fields);

			dictAttachmentFields.add(DictField.builder()
					.id("attachmentField")
					.name("Вложение")
					.type(DictFieldType.ATTACHMENT)
					.required(false)
					.multivalued(false)
					.build());

			dictAttachment.setFields(dictAttachmentFields);

			mongoTemplate.createCollection(dictAttachment.getId(), buildCollectionOptions(dictAttachment));
			dictRepository.save(dictAttachment);

			var refFields = new ArrayList<>(fields);

			refFields.add(
					DictField.builder()
							.id(DICT_ID)
							.name("Ссылка")
							.type(DictFieldType.DICT)
							.required(false)
							.multivalued(false)
							.dictRef(new DictFieldName(DICT_ID, _ID))
							.build()
			);

			refDict.setFields(refFields);

			mongoTemplate.createCollection(refDict.getId(), buildCollectionOptions(refDict));
			dictRepository.save(refDict);

			var doc = new HashMap<String, Object>(
					Map.of(
							"stringField", "string",
							"integerField", 1L,
							"doubleField", Decimal128.parse("2.0"),
							"timestampField", "2021-08-15T06:00:00.000Z",
							"booleanField", true));

			doc.put(CREATED, LocalDateTime.now());
			doc.put(UPDATED, LocalDateTime.now());
			doc.put(DELETED, null);
			doc.put(HISTORY, List.of(new HashMap<>(doc)));
			doc.put(VERSION, 1L);

			mongoTemplate.save(doc, dict.getId());


		}
	}

	private static void addServiceFields(List<DictField> dictFields)
	{
		dictFields.add(0, DictField.builder()
				.id(_ID)
				.name("Идентификатор")
				.type(DictFieldType.STRING)
				.required(false)
				.multivalued(false)
				.build());

		dictFields.add(
				DictField.builder()
						.id(CREATED)
						.name("Дата создания")
						.type(DictFieldType.TIMESTAMP)
						.required(true)
						.multivalued(false)
						.build());

		dictFields.add(
				DictField.builder()
						.id(UPDATED)
						.name("Дата обновления")
						.type(DictFieldType.TIMESTAMP)
						.required(true)
						.multivalued(false)
						.build());

		dictFields.add(
				DictField.builder()
						.id(DELETED)
						.name("Дата удаления")
						.type(DictFieldType.TIMESTAMP)
						.required(false)
						.multivalued(false)
						.build());

		dictFields.add(
				DictField.builder()
						.id(HISTORY)
						.name("История изменений")
						.type(DictFieldType.JSON)
						.required(true)
						.multivalued(true)
						.build());

		dictFields.add(
				DictField.builder()
						.id(VERSION)
						.name("Версия")
						.type(DictFieldType.INTEGER)
						.required(true)
						.multivalued(false)
						.build());
	}

	//fixme: избавиться от дубляжа кода при создании коллекции (скопировано из MongoDictBackend)
	private CollectionOptions buildCollectionOptions(Dict dict)
	{
		return CollectionOptions.empty()
				.schema(buildMongoJsonSchema(dict));
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
					case STRING, DICT -> string(fieldId);
					case BOOLEAN -> named(fieldId).ofType(new JsonType("boolean"));
					case DATE, TIMESTAMP -> date(fieldId);

					default -> throw new RuntimeException();
				};
	}
}
