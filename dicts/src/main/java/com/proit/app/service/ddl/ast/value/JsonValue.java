package com.proit.app.service.ddl.ast.value;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class JsonValue extends Value<String>
{
	private final String value;
}
