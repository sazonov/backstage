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

package com.proit.app.model.domain.audit;

import com.proit.app.conversion.jpa.AuditPropertiesConverter;
import com.proit.app.model.domain.UuidGeneratedEntity;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.Converter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Converter(converterClass = AuditPropertiesConverter.class, name = AuditPropertiesConverter.NAME)
public class Audit extends UuidGeneratedEntity
{
	@Column(length = 512, nullable = false)
	private String type;

	@Column(name = "object_id", length = 40, nullable = false)
	private String objectId;

	@Column(name = "user_id", length = 40)
	private String userId;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "date")
	private LocalDateTime date;

	private boolean success;

	@Basic
	@Column(columnDefinition = "jsonb")
	@Convert(AuditPropertiesConverter.NAME)
	private AuditProperties properties = new AuditProperties();
}
