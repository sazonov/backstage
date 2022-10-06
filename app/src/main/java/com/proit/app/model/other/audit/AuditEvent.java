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

package com.proit.app.model.other.audit;

import com.proit.app.model.domain.audit.AuditPropertiesField;
import com.proit.app.model.domain.audit.AuditPropertiesProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AuditEvent implements Serializable
{
	private String type;

	private String objectId;

	private String userId;

	private LocalDateTime timestamp;

	private boolean success;

	private List<AuditPropertiesField> fields = new ArrayList<>();

	private List<AuditPropertiesProperty> properties = new ArrayList<>();
}
