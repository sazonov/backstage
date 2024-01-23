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

package com.proit.app.dict.service.mongo;

import com.proit.app.dict.configuration.properties.DictsProperties;
import com.proit.app.dict.service.backend.mongo.MongoEngine;
import com.proit.app.dict.service.migration.StorageMigrationService;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link MongoStorage аннотация для обозаначения миграций типов: {@link StorageMigrationService , ClasspathMigrationService } в источник данных MongoDB}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TestPropertySource(properties = {
		DictsProperties.STORAGE_PROPERTY + "=" + MongoEngine.MONGO,
		DictsProperties.DEFAULT_ENGINE_PROPERTY + "=" + MongoEngine.MONGO
})
public @interface MongoStorage
{
}
