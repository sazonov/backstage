package com.proit.app.dict.service.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class TestEnums
{
	@ApiEnum
	@Getter
	@RequiredArgsConstructor
	enum TestEnum0
	{
		TEST_VALUE_0("TEST_VALUE_0"),
		TEST_VALUE_1("TEST_VALUE_1");

		@ApiEnumDescription
		private final String title;
	}

	@ApiEnum
	@Getter
	@RequiredArgsConstructor
	enum TestEnum1
	{
		TEST_VALUE_2("TEST_VALUE_2"),
		TEST_VALUE_3("TEST_VALUE_3");

		@ApiEnumDescription
		private final String title;
	}
}
