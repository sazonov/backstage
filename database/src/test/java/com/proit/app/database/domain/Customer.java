package com.proit.app.database.domain;

import com.proit.app.database.configuration.jpa.eclipselink.annotation.ReadOnlyColumn;
import com.proit.app.database.model.Identity;
import com.proit.app.database.model.UuidGeneratedEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
public class Customer extends UuidGeneratedEntity
{
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "\"order\"",
			joinColumns = @JoinColumn(name = "fk_customer"),
			inverseJoinColumns = @JoinColumn(name = "fk_order"))
	private Set<Product> products = new HashSet<>();

	@ReadOnlyColumn
	@ElementCollection
	@CollectionTable(name = "\"order\"", joinColumns = @JoinColumn(name = "fk_customer"))
	@Column(name = "fk_order")
	private Set<String> productIds = new HashSet<>();

	public void setProducts(Set<Product> products)
	{
		this.products = products;

		productIds = (products != null && !products.isEmpty())
				? products.stream()
				.map(Identity::getId)
				.collect(Collectors.toUnmodifiableSet())
				: Set.of();
	}
}
