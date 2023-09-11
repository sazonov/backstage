package com.proit.app.repository;

import com.proit.app.common.CommonTest;
import com.proit.app.repository.mongo.MongoDictRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MongoDictRepositoryTest extends CommonTest
{
	@Autowired
	private MongoDictRepository mongoDictRepository;

	@Test
	public void checkDictRepository()
	{
		mongoDictRepository.findAll();
	}
}