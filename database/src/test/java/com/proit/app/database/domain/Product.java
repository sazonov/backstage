package com.proit.app.database.domain;

import com.proit.app.database.model.UuidGeneratedEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Product extends UuidGeneratedEntity
{
	private String name;
}
