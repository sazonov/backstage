package com.proit.app.service.imp;

import com.proit.app.common.AbstractTest;

class ImportCsvServiceTest extends AbstractTest
{
//	@Autowired private ImportCsvService importCsvService;
//	@Autowired private DictService dictService;
//
//	//	TODO: заменить на тестовые данные
//	@Test
//	void importDictCorrect() throws IOException
//	{
//		var fields = new ArrayList<DictField>();
//
//		fields.add(
//				DictField.builder()
//						.id("name")
//						.name("строка")
//						.type(DictFieldType.STRING)
//						.required(false)
//						.multivalued(false)
//						.build()
//		);
//
//		fields.add(
//				DictField.builder()
//						.id("inventoryNumber")
//						.name("строка")
//						.type(DictFieldType.STRING)
//						.required(false)
//						.multivalued(false)
//						.build()
//		);
//
//		fields.add(
//				DictField.builder()
//						.id("rfidNumber")
//						.name("строка")
//						.type(DictFieldType.STRING)
//						.required(false)
//						.multivalued(false)
//						.build()
//		);
//
//		fields.add(
//				DictField.builder()
//						.id("description")
//						.name("строка")
//						.type(DictFieldType.STRING)
//						.required(false)
//						.multivalued(false)
//						.build()
//		);
//
//		dictService.create(Dict.builder()
//				.id("mafs")
//				.name("mafs")
//				.fields(fields)
//				.build());
//
//
//		var result = importCsvService.importDict("mafs", new ClassPathResource("kgh_dev_public_maf.csv").getInputStream());
//
//		assertNotNull(result);
//	}
}