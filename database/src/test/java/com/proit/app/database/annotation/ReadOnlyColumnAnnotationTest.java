package com.proit.app.database.annotation;

import com.proit.app.database.AbstractTest;
import com.proit.app.database.domain.Customer;
import com.proit.app.database.domain.Product;
import com.proit.app.database.repository.CustomerRepository;
import com.proit.app.database.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReadOnlyColumnAnnotationTest extends AbstractTest
{
	@Autowired ProductRepository productRepository;
	@Autowired CustomerRepository customerRepository;

	@Autowired TransactionTemplate transactionTemplate;

	@Test
	void readOnlyWithCollectionTableAnnotationWithoutSetReadOnlyAttributeTest()
	{
		transactionTemplate.executeWithoutResult(action ->
				productRepository.saveAll(List.of(
						new Product("apple"),
						new Product("banana"),
						new Product("orange"),
						new Product("pineapple"))
				));

		var savedCustomer = transactionTemplate.execute(action -> {
			var products = productRepository.findAll();
			assertFalse(products.isEmpty());

			var customer = new Customer();
			customer.setProducts(new HashSet<>(products));

			return customerRepository.save(customer);
		});

		var customer = customerRepository.findByIdEx(savedCustomer.getId());

		assertFalse(customer.getProductIds().isEmpty());
	}

	@Test
	void readOnlyWithCollectionTableAnnotationWithReadOnlyAttributeSetTest()
	{
		var result = transactionTemplate.execute(action -> {
			var customer = new Customer();

			return customerRepository.save(customer);
		});

		transactionTemplate.execute(action -> {
			var customer = customerRepository.findByIdEx(result.getId());
			customer.setProductIds(Set.of("banana_id"));

			return customerRepository.save(customer);
		});

		var savedCustomer = customerRepository.findByIdEx(result.getId());

		assertEquals(result.getProductIds(), savedCustomer.getProductIds());
	}
}
