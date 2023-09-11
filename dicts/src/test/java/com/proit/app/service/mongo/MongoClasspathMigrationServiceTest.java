/*
 *
 *  Copyright 2019-2023 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.proit.app.service.mongo;

import com.proit.app.common.TestPipeline;
import com.proit.app.service.CommonClasspathMigrationServiceTest;
import com.proit.app.service.backend.mongo.MongoEngine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

@Order(TestPipeline.MONGO_CLASSPATH_MIGRATION)
@MongoStorage
class MongoClasspathMigrationServiceTest extends CommonClasspathMigrationServiceTest
{
	@BeforeAll
	public void init()
	{
		super.init(MongoEngine.MONGO);
	}

	@Test
	void migrate_WithMultipleEngines()
	{
		migrateMultipleEngines();
	}

	@Test
	void migrate_Exc()
	{
		migrateExc();
	}

	@Test
	void migrate_CommitSuccess()
	{
		migrateCommitSuccess();
	}

	@Test
	void migrate_RollbackSuccess()
	{
		migrateRollbackSuccess();
	}

	@Test
	void migrate_Json()
	{
		migrateJson();
	}

	@Test
	void migrate_GeoJson()
	{
		migrateGeoJson();
	}

	@Test
	void migrate_withSelfJoinDict()
	{
		migrateSelfJoin();
	}
}