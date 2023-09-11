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

package com.proit.app.service;

import com.proit.app.common.CommonTest;
import com.proit.app.constant.ExportedDictFormat;
import com.proit.app.service.export.DictExportService;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommonDictExportServiceTest extends CommonTest
{
	protected static String TESTABLE_DICT_ID;

	@Autowired
	private DictExportService dictExportService;

	public void buildTestableDictHierarchy(String storageDictId)
	{
		TESTABLE_DICT_ID = createNewDict(storageDictId + "export").getId();

		addDictData(TESTABLE_DICT_ID);
	}

	protected void exportJsonNotNullItemsIds()
	{
		var exportedResource = dictExportService.exportToResource(TESTABLE_DICT_ID, ExportedDictFormat.JSON, List.of("stringField"));

		assertTrue(exportedResource.getResource().exists());
		assertTrue(StringUtils.hasText(exportedResource.getFilename()));
	}

	protected void exportJsonNullItemsIds()
	{
		var exportedResource = dictExportService.exportToResource(TESTABLE_DICT_ID, ExportedDictFormat.JSON, null);

		assertTrue(exportedResource.getResource().exists());
		assertTrue(StringUtils.hasText(exportedResource.getFilename()));
	}

	protected void exportJsonNullDictIdNullItemsIds()
	{
		assertThrows(IllegalArgumentException.class, () -> dictExportService.exportToResource(null, ExportedDictFormat.JSON, null));
	}

	protected void exportSqlNotNullItemsIds()
	{
		var exportedResource = dictExportService.exportToResource(TESTABLE_DICT_ID, ExportedDictFormat.SQL, List.of("stringField"));

		assertTrue(exportedResource.getResource().exists());
		assertTrue(StringUtils.hasText(exportedResource.getFilename()));
	}

	protected void exportSqlNullItemsIds()
	{
		var exportedResource = dictExportService.exportToResource(TESTABLE_DICT_ID, ExportedDictFormat.SQL, null);

		assertTrue(exportedResource.getResource().exists());
		assertTrue(StringUtils.hasText(exportedResource.getFilename()));
	}

	protected void exportCvsNotNullItemsIds()
	{
		var exportedResource = dictExportService.exportToResource(TESTABLE_DICT_ID, ExportedDictFormat.CSV, List.of("stringField"));

		assertTrue(exportedResource.getResource().exists());
		assertTrue(StringUtils.hasText(exportedResource.getFilename()));
	}

	protected void exportCvsNullItemsIds()
	{
		var exportedResource = dictExportService.exportToResource(TESTABLE_DICT_ID, ExportedDictFormat.CSV, null);

		assertTrue(exportedResource.getResource().exists());
		assertTrue(StringUtils.hasText(exportedResource.getFilename()));
	}

//	TODO: предусмотреть возможность создавать inner dict с указанными идентификаторами.
//	 Сейчас после создания items в innerDict получают свои идентификаторы
//	protected void exportSqlNullItemsIds() throws IOException
//	{
//		importJsonService.importDict(INDEBTEDNESS_ID,
//				new ClassPathResource("kgh_dev_public_indebtedness.json").getInputStream());
//
//		Resource stringField = dictExportService.exportToResource(INDEBTEDNESS_ID, ExportedDictFormat.SQL, null);
//
//		assertTrue(stringField.exists());
//	}
}
