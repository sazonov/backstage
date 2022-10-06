package com.proit.app.service;

import com.proit.app.common.AbstractTest;
import com.proit.app.constant.ExportedDictFormat;
import com.proit.app.service.export.DictExportService;
import com.proit.app.service.imp.ImportJsonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DictExportServiceTest extends AbstractTest
{
	public static final String INDEBTEDNESS_ID = "indebtedness";

	@Autowired
	DictService dictService;
	@Autowired ImportJsonService importJsonService;
	@Autowired DictExportService dictExportService;

	@Test
	void export_cvsNotNullItemsIdsTest()
	{
		Resource stringField = dictExportService.exportToResource(DICT_ID, ExportedDictFormat.CSV, List.of("stringField"));

		assertTrue(stringField.exists());
	}

	@Test
	void export_cvsNullItemsIdsTest()
	{
		Resource stringField = dictExportService.exportToResource(DICT_ID, ExportedDictFormat.CSV, null);

		assertTrue(stringField.exists());
	}

	//todo: предусмотреть возможность создавать inner dict с указанными идентификаторами.
	//      Сейчас после создания items в innerDict получают свои идентификаторы
//	@Test
//	void export_sqlNullItemsIdsTest() throws IOException
//	{
//
//		importJsonService.importDict(INDEBTEDNESS_ID,
//				new ClassPathResource("kgh_dev_public_indebtedness.json").getInputStream());
//
//		Resource stringField = dictExportService.exportToResource(INDEBTEDNESS_ID, ExportedDictFormat.SQL, null);
//
//		assertTrue(stringField.exists());
//	}

	@Test
	void export_jsonNotNullItemsIdsTest()
	{
		Resource stringField = dictExportService.exportToResource(DICT_ID, ExportedDictFormat.JSON, List.of("stringField"));

		assertTrue(stringField.exists());
	}

	@Test
	void export_jsonNullItemsIdsTest()
	{
		Resource stringField = dictExportService.exportToResource(DICT_ID, ExportedDictFormat.JSON, null);

		assertTrue(stringField.exists());
	}

	@Test
	void export_jsonNullDictIdNullItemsIdsTest()
	{
		assertThrows(IllegalArgumentException.class, () -> dictExportService.exportToResource(null, ExportedDictFormat.JSON, null));
	}
}