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

package com.proit.app.repository;

import com.proit.app.configuration.properties.AttachmentProperties;
import com.proit.app.model.domain.Attachment;
import com.proit.app.repository.generic.CustomJpaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

@ConditionalOnProperty(AttachmentProperties.ACTIVATION_PROPERTY)
public interface AttachmentRepository extends CustomJpaRepository<Attachment, String>
{
	@Query("select distinct a.id from Attachment a left outer join a.bindings ab where ab.attachment is null and a.created < :date")
	List<String> findExpiredUnbounded(@Param("date") LocalDateTime expirationDate, Pageable pageable);
}
