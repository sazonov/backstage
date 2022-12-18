package com.proit.app.service;

import com.proit.app.common.AbstractTest;
import com.proit.app.constant.ServiceFieldConstants;
import com.proit.app.domain.Dict;
import com.proit.app.domain.DictIndex;
import com.proit.app.exception.DictionaryAlreadyExistsException;
import com.proit.app.exception.DictionaryDeletedException;
import com.proit.app.exception.DictionaryNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DictServiceTest extends AbstractTest
{
	public static final String TEST_INDEX_2 = "testIndex2";

	@Autowired
	private DictService dictService;

	@Test
	void getByIdCorrect()
	{
		assertEquals(dict, dictService.getById(DICT_ID));
	}

	@Test
	void getByIdDictDeleted()
	{
		assertThrows(DictionaryDeletedException.class, () -> dictService.getById(DELETED_DICT_ID));
	}

	@Test
	void getByIdDictNotFound()
	{
		assertThrows(DictionaryNotFoundException.class, () -> dictService.getById("incorrect"));
	}

	@Test
	void getAll()
	{
		assertEquals(10, dictService.getAll().size());
	}

	@Test
	void createCorrect()
	{
		dict.setId("test2");

		var fields = dict.getFields();

		dict.setFields(fields.stream().filter(f -> !ServiceFieldConstants.getServiceInsertableFields().contains(f.getId())).collect(Collectors.toList()));

		assertEquals(dict, dictService.create(dict));

		dict.setId(DICT_ID);
		dict.setFields(fields);
	}

	@Test
	void createAlreadyExists()
	{
		var alreadyExistsDict = Dict.builder().id(DICT_ID).build();

		assertThrows(DictionaryAlreadyExistsException.class, () -> dictService.create(alreadyExistsDict));
	}

	@Test
	void deleteCorrect()
	{
		dictService.delete(DICT_ID, true);

		assertThrows(DictionaryDeletedException.class, () -> dictService.getById(DICT_ID));

		dictService.delete(DICT_ID, false);
	}

	@Test
	void deleteDictNotFound()
	{
		assertThrows(DictionaryNotFoundException.class, () -> dictService.delete("incorrect", true));
	}

	@Test
	void update()
	{
		dict.setName("updated");

		var fields = dict.getFields();
		dict.setFields(fields.stream().filter(f -> !ServiceFieldConstants.getServiceSchemeFields().contains(f.getId())).collect(Collectors.toList()));

		assertEquals(dict, dictService.update(dict.getId(), dict));

		dict.setName("тест");
		dict.setFields(fields.stream().filter(f -> !ServiceFieldConstants.getServiceSchemeFields().contains(f.getId())).collect(Collectors.toList()));
		dictService.update(dict.getId(), dict);

		dict.setFields(fields);
	}

	@Test
	void createIndex()
	{
		dictService.createIndex(DICT_ID, DictIndex.builder()
				.id(TEST_INDEX_2)
				.direction(Sort.Direction.ASC)
				.fields(List.of(dict.getFields().get(1).getId()))
				.build());

		assertEquals(dictService.getById(DICT_ID).getIndexes().size(), dict.getIndexes().size() + 1);

		dictService.deleteIndex(DICT_ID, TEST_INDEX_2);
	}

	@Test
	void deleteIndex()
	{
		dictService.deleteIndex(DICT_ID, INDEX_ID);

		var d = dictService.getById(DICT_ID);

		assertEquals(d.getIndexes().size(), dict.getIndexes().size() - 1);

		dictService.createIndex(DICT_ID, DictIndex.builder()
				.id(INDEX_ID)
				.fields(List.of("stringField"))
				.build());
	}
}