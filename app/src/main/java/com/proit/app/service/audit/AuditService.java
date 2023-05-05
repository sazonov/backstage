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

package com.proit.app.service.audit;

import com.proit.app.configuration.properties.AuditProperties;
import com.proit.app.model.domain.audit.Audit;
import com.proit.app.model.dto.audit.AuditEvent;
import com.proit.app.model.other.filter.AuditFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = AuditProperties.ACTIVATION_PROPERTY, matchIfMissing = true)
@RequiredArgsConstructor
public class AuditService
{
	private final AuditStore auditStore;

	public void log(AuditEvent auditEvent)
	{
		auditStore.write(auditEvent);
	}

	public Page<Audit> getByFilter(AuditFilter filter, Pageable pageable)
	{
		return auditStore.getByFilter(filter, pageable);
	}

	public Page<Audit> getBySpecification(Specification<Audit> specification, Pageable pageable)
	{
		return auditStore.getBySpecification(specification, pageable);
	}
}
