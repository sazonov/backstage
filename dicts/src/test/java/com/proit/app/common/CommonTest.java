package com.proit.app.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proit.app.configuration.backend.provider.DictSchemeBackendProvider;
import com.proit.app.configuration.db.MongoConfiguration;
import com.proit.app.configuration.properties.DictsProperties;
import com.proit.app.domain.*;
import com.proit.app.model.dictitem.DictDataItem;
import com.proit.app.service.DictDataService;
import com.proit.app.service.DictService;
import com.proit.app.service.backend.DictBackend;
import com.proit.app.service.backend.DictTransactionBackend;
import com.proit.app.service.backend.VersionSchemeBackend;
import com.proit.app.service.migration.ClasspathMigrationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.util.*;

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
		return dictService.create(buildDict(dictId));
	}

	protected Dict buildDict(String dictId)
	{
		var expectedDict = new Dict();

		expectedDict.setId(withRandom(dictId));
		expectedDict.setFields(buildFields());
		expectedDict.setIndexes(new ArrayList<>(List.of(buildIndex(dictId, "stringField"), buildIndex(dictId, "integerField"))));
		expectedDict.setConstraints(new ArrayList<>(List.of(buildConstraint(dictId, "integerField"))));
		expectedDict.setEnums(new ArrayList<>(List.of(buildEnum(dictId), buildEnum(dictId))));
		expectedDict.setEngine(new DictEngine(DictsProperties.DEFAULT_ENGINE));

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
}
