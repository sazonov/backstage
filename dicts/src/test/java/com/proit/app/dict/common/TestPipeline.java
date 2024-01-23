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

package com.proit.app.dict.common;

public class TestPipeline
{
	public static final int FIRST_INIT = 1;

	public static final int POSTGRES_STORAGE_MIGRATION = 2;
	public static final int POSTGRES_DICT = 3;
	public static final int POSTGRES_DICT_DATA = 4;
	public static final int POSTGRES_DICT_DATA_CONVERSION = 5;
	public static final int POSTGRES_CLASSPATH_MIGRATION = 6;
	public static final int POSTGRES_DICT_VALIDATION = 7;
	public static final int POSTGRES_DICT_DATA_VALIDATION = 8;
	public static final int POSTGRES_MAPPING = 9;
	public static final int POSTGRES_EXPORT = 10;
	public static final int POSTGRES_INTERPRETER = 11;

	public static final int MONGO_STORAGE_MIGRATION = 12;
	public static final int MONGO_DICT = 13;
	public static final int MONGO_DICT_DATA = 14;
	public static final int MONGO_DICT_DATA_CONVERSION = 15;
	public static final int MONGO_CLASSPATH_MIGRATION = 16;
	public static final int MONGO_DICT_VALIDATION = 17;
	public static final int MONGO_DICT_DATA_VALIDATION = 18;
	public static final int MONGO_MAPPING = 19;
	public static final int MONGO_EXPORT = 20;
	public static final int MONGO_INTERPRETER = 21;

	public static final int DICT_GET_ALL_TEST = -100;
	public static final int DICT_GET_BY_ID_TEST = -200;
	public static final int DICT_DATA_GET_BY_FILTER_TEST = -200;
	public static final int DICT_DATA_GET_BY_FILTER_WITH_LOGICAL_EXPRESSION_TEST = -150;
	public static final int DICT_DATA_EXISTS_BY_FILTER_TEST = -100;
}
