/*
 *    Copyright 2019-2024 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.proit.app.utils;

import com.proit.app.model.other.user.Principal;
import com.proit.app.model.other.user.UserInfo;
import com.proit.app.utils.functional.RunnableEx;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Callable;

@UtilityClass
public final class SecurityUtils
{
	public String getCurrentUserId()
	{
		return getCurrentUser().getId();
	}

	public Principal getCurrentUser()
	{
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null)
		{
			return UserInfo.SYSTEM_USER;
		}
		else if (authentication.getPrincipal().equals("anonymousUser"))
		{
			return UserInfo.ANONYMOUS_USER;
		}

		if (!(authentication.getPrincipal() instanceof Principal))
		{
			throw new RuntimeException(String.format("unsupported principal type: %s", authentication.getPrincipal().getClass().getSimpleName()));
		}

		return (Principal) authentication.getPrincipal();
	}

	@SneakyThrows
	public <R> R runForUser(Principal user, Callable<R> callable)
	{
		var currentAuth = SecurityContextHolder.getContext().getAuthentication();

		try
		{
			SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

			return callable.call();
		}
		finally
		{
			SecurityContextHolder.getContext().setAuthentication(currentAuth);
		}
	}

	@SneakyThrows
	public void runForUser(Principal user, RunnableEx runnable)
	{
		runForUser(user, () -> {
			runnable.run();

			return true;
		});
	}

	public <R> Callable<R> callableForUser(Principal user, Callable<R> callable)
	{
		return () -> runForUser(user, callable);
	}
}
