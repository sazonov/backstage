package com.proit.feign.interceptor;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum RequestHeaders
{
	USER_ID("X-Feign-User");

	private final String name;
}
