package com.proit.app.service.imp;

import com.proit.app.common.AbstractTest;

class ImportJsonServiceTest extends AbstractTest
{
//	public static final String USER_ID = "user";
//	public static final String INDEBTEDNESS_ID = "indebtedness";
//
//	public static final String FK_USER = "fk_user";
//
//	@Autowired DictService dictService;
//	@Autowired DictDataService dictDataService;
//	@Autowired ImportJsonService importJsonService;
//
//	@Test
//	void importDict() throws IOException
//	{
//		List<DictItem> indebtedness = importJsonService.importDict(INDEBTEDNESS_ID,
//				new ClassPathResource("kgh_dev_public_indebtedness.json").getInputStream());
//
//		importJsonService.importDict(INDEBTEDNESS_ID, new ClassPathResource("kgh_dev_public_indebtedness_2.json").getInputStream());
//
//		var result = dictDataService.getByFilter(INDEBTEDNESS_ID, List.of("*", USER_ID + ".*"), null, PageRequest.of(0, 10));
//
//		assertAll(() -> {
//			assertFalse(indebtedness.isEmpty());
//			assertNotNull(result.getContent().get(0).getData().get(FK_USER));
//		});
//	}
}