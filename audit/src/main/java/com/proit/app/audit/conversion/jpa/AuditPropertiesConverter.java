/*
 *    Copyright 2019-2023 the original author or authors.
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

package com.proit.app.audit.conversion.jpa;

import com.fasterxml.jackson.core.type.TypeReference;
import com.proit.app.audit.model.domain.AuditProperties;
import com.proit.app.database.conversion.jpa.AbstractMutableJsonConverter;

public class AuditPropertiesConverter extends AbstractMutableJsonConverter<AuditProperties>
{
	public static final String NAME = "auditPropertiesConverter";

	@Override
	protected TypeReference<AuditProperties> createTypeReference()
	{
		return new TypeReference<>() { };
	}

	@Override
	protected AuditProperties createDefaultValue()
	{
		return new AuditProperties();
	}
}
