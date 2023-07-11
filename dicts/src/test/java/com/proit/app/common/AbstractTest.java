package com.proit.app.common;

import com.proit.app.configuration.db.MongoConfiguration;
import com.proit.app.domain.*;
import com.proit.app.model.dictitem.DictDataItem;
import com.proit.app.service.DictDataService;
import com.proit.app.service.DictService;
import org.bson.types.Decimal128;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;

import static com.proit.app.constant.ServiceFieldConstants._ID;

@SpringBootTest
@ContextConfiguration(initializers = {AppDataSourceInitializer.class, MongoInitializer.class})
@Import({JacksonAutoConfiguration.class, MongoConfiguration.class})
public class AbstractTest
{
	public static final String DICT_ID = "test";
	public static final String REF_DICT_ID = "refDict";
	public static final String DELETED_DICT_ID = "deletedDict";
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
	protected DictService dictService;

	@Autowired
	protected DictDataService dictDataService;

	@PostConstruct
	void init()
	{
		var testableDictIds = Set.of(DICT_ID, DELETED_DICT_ID, REF_DICT_ID);

		var firstInit = dictService.getAll()
				.stream()
				.map(Dict::getId)
				.noneMatch(testableDictIds::contains);

		if (firstInit)
		{
			createDict();
			createDeletedDict();
			createDictAttachment();
			createRefDict();
			addDictData();
		}
	}

	private void createDict()
	{
		dict.setFields(buildFields());

		var index = DictIndex.builder()
				.id(INDEX_ID)
				.fields(List.of("stringField"))
				.direction(Sort.Direction.ASC)
				.build();

		dict.setIndexes(List.of(index));

		dictService.create(dict);
	}

	private void createDeletedDict()
	{
		deletedDict.setFields(buildFields());

		dictService.create(deletedDict);
	}

	private void createDictAttachment()
	{
		var dictAttachmentFields = buildFields();

		dictAttachmentFields.add(DictField.builder()
				.id("attachmentField")
				.name("Вложение")
				.type(DictFieldType.ATTACHMENT)
				.required(false)
				.multivalued(false)
				.build()
		);

		dictAttachment.setFields(dictAttachmentFields);

		dictService.create(dictAttachment);
	}

	private void createRefDict()
	{
		var refFields = buildFields();

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

		dictService.create(refDict);
	}

	private void addDictData()
	{
		var dataMap = new HashMap<String, Object>(
				Map.of(
						"stringField", "string",
						"integerField", 1L,
						"doubleField", Decimal128.parse("2.0"),
						"timestampField", "2021-08-15T06:00:00.000Z",
						"booleanField", true)
		);

		dictDataService.create(DictDataItem.of(dict.getId(), dataMap));
	}

	private List<DictField> buildFields()
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

		return fields;
	}
}
