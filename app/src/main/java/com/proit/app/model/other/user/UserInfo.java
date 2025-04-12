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

package com.proit.app.model.other.user;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserInfo implements Principal
{
	public static UserInfo ANONYMOUS_USER;
	public static UserInfo SYSTEM_USER;

	public static final String ANONYMOUS_USER_ID = "anonymous";
	public static final String ANONYMOUS_USER_NAME = "anonymous";

	public static final String SYSTEM_USER_ID = "system";
	public static final String SYSTEM_USER_NAME = "system";

	static
	{
		ANONYMOUS_USER = new UserInfo();

		ANONYMOUS_USER.id = ANONYMOUS_USER_ID;
		ANONYMOUS_USER.type = UserType.LOCAL;
		ANONYMOUS_USER.email = ANONYMOUS_USER_ID + "@local";
		ANONYMOUS_USER.firstName = ANONYMOUS_USER_NAME;
		ANONYMOUS_USER.lastName = ANONYMOUS_USER_NAME;

		SYSTEM_USER = new UserInfo();

		SYSTEM_USER.id = SYSTEM_USER_ID;
		SYSTEM_USER.type = UserType.LOCAL;
		SYSTEM_USER.email = SYSTEM_USER_ID + "@local";
		SYSTEM_USER.firstName = SYSTEM_USER_NAME;
		SYSTEM_USER.lastName = SYSTEM_USER_NAME;
	}

	private String id;

	private UserType userType;

	private String firstName;

	private String middleName;

	private String lastName;

	private String password;

	private String email;

	private boolean emailVerified;

	private String phone;

	private boolean phoneVerified;

	private Object details;

	private UserType type;

	private LocalDateTime created;

	private LocalDateTime updated;

	private LocalDateTime deleted;

	private LocalDateTime blocked;

	public <T> T getDetails(Class<T> clazz)
	{
		if (details != null)
		{
			return clazz.cast(details);
		}

		return null;
	}
}
