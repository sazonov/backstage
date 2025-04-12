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

package com.proit.app.attachment.model.domain;

import com.proit.app.database.model.UuidGeneratedEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.eclipse.persistence.annotations.Index;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "attachment", schema = "#{@attachmentProperties.ddl.scheme}")
public class Attachment extends UuidGeneratedEntity
{
	@Index(name = "ix_attachment_user_id")
	@Column(name = "user_id")
	private String userId;

	@Column(nullable = false)
	private LocalDateTime created;

	private LocalDateTime updated;

	@Column(name = "mime_type", nullable = false)
	private String mimeType;

	@Column(name = "file_name")
	private String fileName;

	@Column(nullable = false)
	private Integer size;

	private String checksum;

	@Builder.Default
	@OneToMany(mappedBy = "attachment")
	private List<AttachmentBinding> bindings = new ArrayList<>();
}
