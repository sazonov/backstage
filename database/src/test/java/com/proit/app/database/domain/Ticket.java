package com.proit.app.database.domain;

import com.proit.app.database.model.UuidGeneratedEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(schema = "#{@postgresTestProperties.ddl.scheme}")
@SecondaryTable(name = "ticket_information", pkJoinColumns = @PrimaryKeyJoinColumn(name = "fk_ticket"), schema = "#{@postgresTestProperties.ddl.scheme}")
public class Ticket extends UuidGeneratedEntity
{
	@Column(nullable = false)
	private LocalDateTime created;

	private String title;

	@Column(table = "ticket_information")
	private String description;

	@Column(name = "sender_email", nullable = false, table = "ticket_information")
	private String senderEmail;
}
