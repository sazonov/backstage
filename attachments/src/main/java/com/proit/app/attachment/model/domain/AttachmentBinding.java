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
import lombok.Getter;
import lombok.Setter;
import org.eclipse.persistence.annotations.Index;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "attachment_binding", schema = "#{@attachmentProperties.ddl.scheme}")
public class AttachmentBinding extends UuidGeneratedEntity
{
	@Column(nullable = false)
	private LocalDateTime created;

	@Index(name = "ix_attachment_binding_type")
	@Column
	private String type;

	@Index(name = "ix_attachment_binding_object_id")
	@Column(name = "object_id")
	private String objectId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fk_attachment")
	private Attachment attachment;

	@Column(name = "fk_attachment", insertable = false, updatable = false)
	private String attachmentId;

	@Index(name = "ix_attachment_binding_user_id")
	@Column(name = "user_id")
	private String userId;

	public void setAttachment(Attachment attachment)
	{
		this.attachment = attachment;
		this.attachmentId = attachment != null ? attachment.getId() : null;
	}
}
