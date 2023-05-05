package com.proit.feign.provider;

import com.proit.app.model.other.user.Principal;

public class DefaultPrincipalProvider implements PrincipalProvider
{
	@Override
	public Principal getPrincipal(String userId)
	{
		return () -> userId;
	}
}
