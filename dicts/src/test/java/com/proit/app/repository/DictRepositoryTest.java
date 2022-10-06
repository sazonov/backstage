package com.proit.app.repository;

import com.proit.app.common.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DictRepositoryTest extends AbstractTest
{
	@Autowired private DictRepository dictRepository;

	@Test
	public void checkDictRepository()
	{
		dictRepository.findAll();
	}
}