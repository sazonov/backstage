package com.proit.app.database;

import com.proit.app.database.domain.UuidTable;
import com.proit.app.database.domain.VarcharTable;
import com.proit.app.database.repository.UuidTableRepository;
import com.proit.app.database.repository.VarcharTableRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты проверяют работоспособность конвертации uuid.
 * В энтити все так же остается java.lang.String
 *
 * @see com.proit.app.database.configuration.jpa.eclipselink.uuid.UuidSupportExtension
 */
class UuidSupportExtensionTest extends AbstractTest
{
	@Autowired
	UuidTableRepository repository;

	@Autowired
	VarcharTableRepository varcharTableRepository;

	@Autowired
	TransactionTemplate transactionTemplate;

	@Test
	void manyToOneEntityAsIdSelectTest()
	{
		assertThrowsExactly(NoSuchElementException.class,
				() -> varcharTableRepository.findByUuidTableId(UUID.randomUUID().toString())
						.orElseThrow());
	}

	@Test
	void insertUuidColumnIdTest()
	{
		var saved = transactionTemplate.execute(status -> {

			var entity = new UuidTable();
			entity.setRandom(Double.MAX_VALUE);

			var list = List.of(new VarcharTable(), new VarcharTable());
			list = varcharTableRepository.saveAll(list);

			entity.setVarcharTables(list);
			entity.getVarcharTableMany().addAll(list);

			return repository.save(entity);
		});

		assertFalse(saved.getVarcharTables().isEmpty());
		assertFalse(saved.getVarcharTableMany().isEmpty());
	}

	@Test
	void selectUuidColumnIdTest()
	{
		var saved = transactionTemplate.execute(status -> {
			var entity = new UuidTable();
			entity.setRandom(Double.MAX_VALUE);

			var list = List.of(new VarcharTable(), new VarcharTable());
			list = varcharTableRepository.saveAll(list);

			entity.setVarcharTables(list);

			return repository.save(entity);
		});

		var table = repository.findByIdEx(saved.getId());
		var first = table.getVarcharTables()
				.stream()
				.findFirst()
				.orElseThrow();

		assertFalse(table.getVarcharTables().isEmpty());
        assertNotNull(first.getUuidTableId());
	}

	@Test
	void updateUuidColumnIdTest()
	{
		var entity = new UuidTable();
		entity.setRandom(Double.MAX_VALUE);

		transactionTemplate.execute(status ->
				repository.saveAndFlush(entity));

		transactionTemplate.executeWithoutResult(status -> {
			var uuidTable = repository.findByIdEx(entity.getId());
			uuidTable.setRandom(Double.MIN_VALUE);
		});
	}

	@Test
	void deleteUuidColumnIdTest()
	{
		var entity = new UuidTable();
		entity.setRandom(Double.MAX_VALUE);

		transactionTemplate.executeWithoutResult(status ->
				repository.saveAndFlush(entity));

		transactionTemplate.executeWithoutResult(status ->
				repository.deleteById(entity.getId()));
	}

	@Test
	void insertVarcharColumnIdTest()
	{
		var entity = new VarcharTable();
		entity.setRandom(Double.MAX_VALUE);

		varcharTableRepository.saveAndFlush(entity);
	}

	@Test
	void updateVarcharColumnIdTest()
	{
		var entity = new VarcharTable();
		entity.setRandom(Double.MAX_VALUE);

		transactionTemplate.execute(status ->
				varcharTableRepository.saveAndFlush(entity));

		transactionTemplate.executeWithoutResult(status -> {
			var uuidTable = varcharTableRepository.findByIdEx(entity.getId());
			uuidTable.setRandom(Double.MIN_VALUE);
		});
	}

	@Test
	void deleteVarcharColumnIdTest()
	{
		var entity = new VarcharTable();
		entity.setRandom(Double.MAX_VALUE);

		transactionTemplate.executeWithoutResult(status ->
				varcharTableRepository.saveAndFlush(entity));

		transactionTemplate.executeWithoutResult(status ->
				varcharTableRepository.deleteById(entity.getId()));
	}
}
