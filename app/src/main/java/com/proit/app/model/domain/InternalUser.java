/*
 *    Copyright 2019-2022 the original author or authors.
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

package com.proit.app.model.domain;

import com.proit.app.conversion.jpa.GenericObjectConverter;
import com.proit.app.model.other.user.UserType;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.Converter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "\"user\"")
@Converter(converterClass = GenericObjectConverter.class ,name = "userDetailsConverter")
public class InternalUser extends UuidGeneratedEntity
{
	@Column(name = "first_name", nullable = false)
	private String firstName;

	@Column(name = "middle_name")
	private String middleName;

	@Column(name = "last_name")
	private String lastName;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "email_verified", nullable = false)
	private boolean emailVerified;

	@Column(name = "phone", unique = true)
	private String phone;

	@Column(name = "phone_verified", nullable = false)
	private boolean phoneVerified;

	@Convert("userDetailsConverter")
	private Object details;

	@Enumerated(EnumType.STRING)
	private UserType type;

	@Temporal(TemporalType.TIMESTAMP)
	private LocalDateTime created;

	@Temporal(TemporalType.TIMESTAMP)
	private LocalDateTime updated;

	@Temporal(TemporalType.TIMESTAMP)
	private LocalDateTime deleted;

	@Temporal(TemporalType.TIMESTAMP)
	private LocalDateTime blocked;

	public void setEmail(String email)
	{
		this.email = email != null ? email.toLowerCase() : null;
	}

	public <T> T getDetails(Class<T> clazz)
	{
		if (details != null)
		{
			return clazz.cast(details);
		}

		return null;
	}
}
