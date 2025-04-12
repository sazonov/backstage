package com.proit.app.database.domain;

import com.proit.app.database.model.UuidGeneratedEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "uuid_table")
public class UuidTable extends UuidGeneratedEntity
{
	private Double random;

	@OneToMany
	@JoinColumn(name = "fk_uuid_table")
	private List<VarcharTable> varcharTables = new ArrayList<>();

	@ManyToMany
	@JoinTable(name = "uuid_table_varchar_table",
			joinColumns = @JoinColumn(name = "fk_uuid_table"),
			inverseJoinColumns = @JoinColumn(name = "fk_varchar_table"))
	private Set<VarcharTable> varcharTableMany = new HashSet<>();
}
