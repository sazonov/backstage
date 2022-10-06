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

package com.proit.app.repository;

import com.proit.app.configuration.properties.AuditProperties;
import com.proit.app.model.domain.audit.Audit;
import com.proit.app.repository.generic.CustomJpaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

@ConditionalOnProperty(value = AuditProperties.ACTIVATION_PROPERTY, matchIfMissing = true)
public interface AuditRepository extends CustomJpaRepository<Audit, String>, JpaSpecificationExecutor<Audit>
{
	List<Audit> findAllByTypeAndObjectId(String type, String objectId);

	List<Audit> findAllByTypeInAndObjectId(List<String> types, String objectId);
}
