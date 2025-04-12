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

package com.proit.app.audit.service;

import com.proit.app.audit.model.domain.Audit;
import com.proit.app.audit.model.dto.AuditEvent;
import com.proit.app.audit.model.other.AuditFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface AuditStore
{
	void write(AuditEvent event);

	Page<Audit> getByFilter(AuditFilter filter, Pageable pageable);

	Page<Audit> getBySpecification(Specification<Audit> specification, Pageable pageable);
}
