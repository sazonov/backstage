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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fk_attachment", nullable = false)
	private Attachment attachment;

	@Setter(AccessLevel.NONE)
	@Column(name = "fk_attachment", insertable = false, updatable = false)
	private String attachmentId;

	private LocalDateTime updated;

	private LocalDateTime created;

	private LocalDateTime deleted;

	public void setAttachment(Attachment attachment)
	{
		this.attachment = attachment;
		this.attachmentId = attachment != null ? attachment.getId() : null;
	}

	@PrePersist
	private void setUpAutocompleteFields()
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
