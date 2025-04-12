package com.proitr;

import com.proit.app.service.enums.ApiEnum;
import com.proit.app.service.enums.ApiEnumDescription;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@ApiEnum
@Getter
@RequiredArgsConstructor
public enum TestEnumInOtherPackage
{
	VALUE("value");

	@ApiEnumDescription
	private final String title;
}
