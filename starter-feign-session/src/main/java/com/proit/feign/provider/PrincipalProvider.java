package com.proit.feign.provider;

import com.proit.app.model.other.user.Principal;

public interface PrincipalProvider
{
	Principal getPrincipal(String userId);
}
