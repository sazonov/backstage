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

package com.proit.app.model.domain;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "document_template")
public class DocumentTemplate extends UuidGeneratedEntity
{
	private String name;

	@Column(name = "attachment_id", insertable = false, updatable = false)
	private String attachmentId;

	private LocalDateTime updated;

	private LocalDateTime created;

	private LocalDateTime deleted;

	@PrePersist
	private void setupAutocompleteFields()
	{
		setUpdated(LocalDateTime.now());
		setCreated(LocalDateTime.now());
	}

	@PreUpdate
	private void setUpdated()
	{
		this.setUpdated(LocalDateTime.now());
	}
}
