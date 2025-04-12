package com.proit.app.database.domain;

import com.proit.app.database.model.UuidGeneratedEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product", schema = "${app.test.postgres.ddl.scheme}")
public class Product extends UuidGeneratedEntity
{
	private String name;
}
