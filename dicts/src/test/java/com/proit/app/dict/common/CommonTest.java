package com.proit.app.dict.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proit.app.database.configuration.db.MongoConfiguration;
import com.proit.app.dict.api.domain.DictFieldType;
import com.proit.app.dict.configuration.backend.provider.DictSchemeBackendProvider;
import com.proit.app.dict.configuration.properties.DictsProperties;
import com.proit.app.dict.domain.*;
import com.proit.app.dict.model.dictitem.DictDataItem;
import com.proit.app.dict.service.DictDataService;
import com.proit.app.dict.service.DictService;
import com.proit.app.dict.service.backend.DictBackend;
import com.proit.app.dict.service.backend.DictTransactionBackend;
import com.proit.app.dict.service.backend.VersionSchemeBackend;
import com.proit.app.dict.service.migration.ClasspathMigrationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.util.*;

import static com.proit.app.dict.constant.ServiceFieldConstants.getServiceSchemeFields;

@SpringBootTest
@ContextConfiguration(initializers = {AppDataSourceInitializer.class, MongoInitializer.class})
@Import({JacksonAutoConfiguration.class, MongoConfiguration.class})
public class CommonTest
{
	public static final String MONGO_DICT_ID = "mgDict";
	public static final String POSTGRES_DICT_ID = "pgDict";

	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	protected DictsProperties dictsProperties;

	@Autowired
	protected DictService dictService;

	@Autowired
	protected DictDataService dictDataService;

	@Autowired
	protected ClasspathMigrationService classpathMigrationService;

	@Autowired
	protected DictSchemeBackendProvider schemeBackendProvider;

	@Autowired
	protected DictBackend dictBackend;

	@Autowired
	protected VersionSchemeBackend versionSchemeBackend;

	@Autowired
	protected DictTransactionBackend dictTransactionBackend;

	protected Dict createNewDict(String dictId)
	{
		return createNewDict(dictId, new DictEngine(DictsProperties.DEFAULT_ENGINE));
	}

	protected Dict createNewDict(String dictId, DictEngine dictEngine)
	{
		return dictService.create(buildDict(dictId, dictEngine));
	}

	protected Dict buildDict(String dictId)
	{
		return buildDict(dictId, new DictEngine(DictsProperties.DEFAULT_ENGINE));
	}

	protected Dict buildDict(String dictId, DictEngine dictEngine)
	{
		var expectedDict = new Dict();

		expectedDict.setId(withRandom(dictId));
		expectedDict.setFields(buildFields());
		expectedDict.setIndexes(new ArrayList<>(List.of(buildIndex(dictId, "stringField"), buildIndex(dictId, "integerField"))));
		expectedDict.setConstraints(new ArrayList<>(List.of(buildConstraint(dictId, "integerField"))));
		expectedDict.setEnums(new ArrayList<>(List.of(buildEnum(dictId), buildEnum(dictId))));
		expectedDict.setEngine(dictEngine);

		return expectedDict;
	}

	protected List<DictField> buildFields()
	{
		var fields = new ArrayList<DictField>();

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
						.required(false)
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

		return fields;
	}

	protected DictIndex buildIndex(String dictId, String... fieldIds)
	{
		return DictIndex.builder()
				.id(withRandom(dictId))
				.direction(Sort.Direction.DESC)
				.fields(Arrays.asList(fieldIds))
				.build();
	}

	protected DictEnum buildEnum(String dictId)
	{
		return DictEnum.builder()
				.id(withRandom(dictId))
				.name("enum_name_" + dictId)
				.values(Set.of("value_1_" + dictId, "value_2_" + dictId))
				.build();
	}

	protected DictConstraint buildConstraint(String dictId, String... fieldIds)
	{
		return DictConstraint.builder()
				.id(withRandom(dictId))
				.fields(Arrays.asList(fieldIds))
				.build();
	}

	protected void addDictData(String dictId)
	{
		var dataMap = new HashMap<String, Object>(
				Map.of(
						"stringField", "string",
						"integerField", 1L,
						"doubleField", BigDecimal.valueOf(Double.parseDouble("2.776")),
						"timestampField", "2021-08-15T06:00:00.000Z",
						"booleanField", true)
		);

		dictDataService.create(DictDataItem.of(dictId, dataMap));
	}

	protected String withRandom(String dictId)
	{
		return "%s%s".formatted(dictId, RandomStringUtils.random(3, true, false));
	}

	protected List<DictField> withoutServiceFields(List<DictField> source)
	{
		return source.stream()
				.filter(it -> !getServiceSchemeFields().contains(it.getId()))
				.toList();
	}

	protected String generateRandomUUIDWithoutDashes()
	{
		return UUID.randomUUID()
				.toString()
				.replace("-", "");
	}
}
