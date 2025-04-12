package com.proit.app.database.repository;

import com.proit.app.database.domain.VarcharTable;

import java.util.Optional;

public interface VarcharTableRepository extends CustomJpaRepository<VarcharTable, String>
{
	Optional<VarcharTable> findByUuidTableId(String tableId);
}
