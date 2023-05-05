/*
 *    Copyright 2019-2023 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.proit.app.configuration.db;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.proit.app.configuration.properties.MongoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MongoProperties.class)
@EnableMongoRepositories(basePackages = "com.proit.app.repository")
@ConditionalOnProperty("app.mongo.host")
public class MongoConfiguration extends AbstractMongoClientConfiguration
{
	private final MongoProperties mongoProperties;

	@Bean
	MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory dbFactory)
	{
		return new MongoTransactionManager(dbFactory);
	}

	@Override
	protected String getDatabaseName()
	{
		return mongoProperties.getDatabase();
	}

	@Override
	protected void configureClientSettings(MongoClientSettings.Builder builder)
	{
		if (mongoProperties.getUsername() != null)
		{
			builder.credential(MongoCredential.createCredential(mongoProperties.getUsername(), "admin", mongoProperties.getPassword().toCharArray()));
		}

		builder.applyToClusterSettings(settings -> settings.hosts(List.of(new ServerAddress(mongoProperties.getHost(), mongoProperties.getPort()))))
				.applyToConnectionPoolSettings(settings -> settings.minSize(mongoProperties.getMinPoolSize()));
	}
}
