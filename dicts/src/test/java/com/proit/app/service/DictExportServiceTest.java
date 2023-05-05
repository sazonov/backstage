package com.proit.app.service;

import com.proit.app.common.AbstractTest;
import com.proit.app.constant.ExportedDictFormat;
import com.proit.app.service.export.DictExportService;
import com.proit.app.service.imp.ImportJsonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictExportServiceTest extends AbstractTest
{
	public static final String INDEBTEDNESS_ID = "indebtedness";

	@Autowired
	private ImportJsonService importJsonService;
	@Autowired
	private DictExportService dictExportService;

	@Test
	void export_cvsNotNullItemsIdsTest()
	{
		var exportedResource = dictExportService.exportToResource(DICT_ID, ExportedDictFormat.CSV, List.of("stringField"));

		assertTrue(exportedResource.getResource().exists());
		assertTrue(StringUtils.hasText(exportedResource.getFilename()));
	}

	@Test
	void export_cvsNullItemsIdsTest()
	{
		var exportedResource = dictExportService.exportToResource(DICT_ID, ExportedDictFormat.CSV, null);

		assertTrue(exportedResource.getResource().exists());
		assertTrue(StringUtils.hasText(exportedResource.getFilename()));
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
		var exportedResource = dictExportService.exportToResource(DICT_ID, ExportedDictFormat.JSON, List.of("stringField"));

		assertTrue(exportedResource.getResource().exists());
		assertTrue(StringUtils.hasText(exportedResource.getFilename()));
	}

	@Test
	void export_jsonNullItemsIdsTest()
	{
		var exportedResource = dictExportService.exportToResource(DICT_ID, ExportedDictFormat.JSON, null);

		assertTrue(exportedResource.getResource().exists());
		assertTrue(StringUtils.hasText(exportedResource.getFilename()));
	}

	@Test
	void export_jsonNullDictIdNullItemsIdsTest()
	{
		assertThrows(IllegalArgumentException.class, () -> dictExportService.exportToResource(null, ExportedDictFormat.JSON, null));
	}

	@Test
	void export_sqlNotNullItemsIdsTest()
	{
		var exportedResource = dictExportService.exportToResource(DICT_ID, ExportedDictFormat.SQL, List.of("stringField"));

		assertTrue(exportedResource.getResource().exists());
		assertTrue(StringUtils.hasText(exportedResource.getFilename()));
	}

	@Test
	void export_sqlNullItemsIdsTest()
	{
		var exportedResource = dictExportService.exportToResource(DICT_ID, ExportedDictFormat.SQL, null);

		assertTrue(exportedResource.getResource().exists());
		assertTrue(StringUtils.hasText(exportedResource.getFilename()));
	}
}