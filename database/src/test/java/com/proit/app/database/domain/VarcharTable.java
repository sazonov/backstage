package com.proit.app.database.domain;

import com.proit.app.database.model.UuidGeneratedEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "varchar_table")
public class VarcharTable extends UuidGeneratedEntity
{
	private Double random;

	@ManyToOne
	@JoinColumn(name = "fk_uuid_table")
	private UuidTable uuidTable;

	@Setter(AccessLevel.NONE)
	@Column(name = "fk_uuid_table", insertable = false, updatable = false)
	private String uuidTableId;
}
