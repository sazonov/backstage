package com.proit.app.service;

import com.proit.app.common.AbstractTest;
import com.proit.app.domain.Dict;
import com.proit.app.domain.DictField;
import com.proit.app.domain.DictFieldName;
import com.proit.app.domain.DictFieldType;
import com.proit.app.exception.*;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationServiceTest extends AbstractTest
{
	public static final List<DictField> fields = new ArrayList<>();
	public static final List<DictField> incorrectFields = new ArrayList<>();

	public static final Dict dictScheme = Dict.builder()
			.id("test1")
			.name("тест")
			.build();

	@Autowired
	private ValidationService validationService;

	public ValidationServiceTest()
	{
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
						.id("timestampField")
						.name("Дата и время")
						.type(DictFieldType.TIMESTAMP)
						.required(true)
						.multivalued(false)
						.build()
		);

		incorrectFields.add(DictField.builder()
				.id("created")
				.name("Дата и время")
				.type(DictFieldType.TIMESTAMP)
				.required(true)
				.multivalued(false)
				.build());
	}

	@Test
	void validateSchemeCorrect()
	{
		dictScheme.setFields(fields);

		validationService.validateScheme(dictScheme);
	}

	@Test
	void validateSchemeForbiddenField()
	{
		dictScheme.setFields(incorrectFields);

		assertThrows(ForbiddenFieldNameException.class, () -> validationService.validateScheme(dictScheme));
	}

	@Test
	void validateSelectFieldsCorrect()
	{
		validationService.validateSelectFields(REF_DICT_ID, List.of(new DictFieldName(DICT_ID, "integerField"),
				new DictFieldName(DICT_ID, "created"), new DictFieldName(DICT_ID, "stringField")));
	}

	@Test
	void validateSelectFieldsFieldNotExisted()
	{
		assertThrows(FieldNotFoundException.class,
				() -> validationService.validateSelectFields(REF_DICT_ID, List.of(new DictFieldName(DICT_ID, "stringField"),
						new DictFieldName(DICT_ID, "incorrect"))));
	}

	@Test
	void validateSelectFieldsIncorrect()
	{
		assertThrows(UnavailableDictRefException.class, () ->
				validationService.validateSelectFields(DICT_ID, List.of(new DictFieldName(DICT_ID, "integerField"))));
	}

	@Test
	void validateSelectFieldsDictNotExisted()
	{
		assertThrows(DictionaryNotFoundException.class, () -> validationService.validateSelectFields("incorrect", List.of()));
	}


	@Test
	void validateDocInsertCorrect()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"integerField", 1,
				"doubleField", Decimal128.parse("2.0"),
				"timestampField", "2021-08-15T06:00:00.000Z");

		validationService.validateDocInsert(DICT_ID, map);
	}

	@Test
	void validateDocInsertDictNotExisted()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"integerField", 1,
				"timestampField", "2021-08-15T06:00:00.000Z");

		assertThrows(DictionaryNotFoundException.class, () -> validationService.validateDocInsert("incorrect", map));
	}

	@Test
	void validateDocInsertNoRequiredField()
	{
		Map<String, Object> map = Map.of("stringField", "string");

		var e = assertThrows(FieldValidationException.class, () -> validationService.validateDocInsert(DICT_ID, map));
		assertEquals(e.getMessage(), "Отсутствует обязательное поле: integerField.");
	}

	@Test
	void validateDocInsertExistsField()
	{
		Map<String, Object> duplicatedFiled = Map.of(
				"stringField11", "text",
				"timestampField", "2021-08-15T06:00:00.000Z");

		assertThrows(ForbiddenFieldNameException.class, () -> validationService.validateDocInsert(DICT_ID, duplicatedFiled));
	}

	@Test
	void validateDocInsertUnexpectedMultivalued()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"integerField", List.of(1, 2),
				"timestampField", "2021-08-15T06:00:00.000Z");

		var e = assertThrows(FieldValidationException.class, () -> validationService.validateDocInsert(DICT_ID, map));
		assertEquals(e.getMessage(), "Не может быть массивом: integerField.");
	}

	@Test
	void validateDocInsertFieldCastException()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"integerField", 1,
				"doubleField", Decimal128.parse("2.0"),
				"timestampField", "incorrect");

		var e = assertThrows(FieldValidationException.class, () -> validationService.validateDocInsert(DICT_ID, map));
		assertEquals(e.getMessage(), "Некорректный формат данных: timestampField.");
	}

	@Test
	void validateDocInsertForbiddenField()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"created", "incorrect",
				"integerField", 1,
				"timestampField", "2021-08-15T06:00:00.000Z");

		assertThrows(ForbiddenFieldNameException.class, () -> validationService.validateDocInsert(DICT_ID, map));
	}
}