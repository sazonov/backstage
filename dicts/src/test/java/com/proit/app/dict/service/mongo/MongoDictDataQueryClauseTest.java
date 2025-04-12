package com.proit.app.dict.service.mongo;

import com.proit.app.dict.common.CommonTest;
import com.proit.app.dict.constant.ServiceFieldConstants;
import com.proit.app.dict.service.backend.mongo.clause.MongoDictDataQueryClause;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MongoDictDataQueryClauseTest extends CommonTest
{
	@Autowired
	private MongoDictDataQueryClause mongoDictDataQueryClause;

	@Test
	void buildSortWithMultiplyOrders()
	{
		var requestOrders = List.of(Sort.Order.desc("integerField"), Sort.Order.desc("doubleField"));

		var sort = mongoDictDataQueryClause.buildSort(Sort.by(requestOrders), Set.of());

		var orders = sort.get()
				.collect(Collectors.toSet());

		var expectedOrders = new ArrayList<>(requestOrders);
		expectedOrders.add(Sort.Order.asc(ServiceFieldConstants._ID));

		assertEquals(orders.size(), 3);
		assertTrue(orders.containsAll(expectedOrders));
	}

	@Test
	void buildSortWithIdDescendingOrders()
	{
		var requestOrders = List.of(Sort.Order.desc(ServiceFieldConstants._ID));

		var sort = mongoDictDataQueryClause.buildSort(Sort.by(requestOrders), Set.of());

		var orders = sort.get()
				.collect(Collectors.toSet());

		assertEquals(orders.size(), 1);
		assertTrue(orders.containsAll(requestOrders));
	}

	@Test
	void buildSortWithoutOrders()
	{
		var sort = mongoDictDataQueryClause.buildSort(Sort.unsorted(), Set.of());

		var orders = sort.get()
				.collect(Collectors.toSet());

		assertEquals(orders.size(), 1);
		assertTrue(orders.contains(Sort.Order.asc(ServiceFieldConstants._ID)));
	}
}
