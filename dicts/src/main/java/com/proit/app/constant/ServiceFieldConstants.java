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

package com.proit.app.constant;

import lombok.Getter;

import java.util.Set;

@Deprecated
public class ServiceFieldConstants
{
	public static final String ID = "id";
	public static final String _ID = "_id";
	public static final String HISTORY = "history";
	public static final String CREATED = "created";
	public static final String UPDATED = "updated";
	public static final String DELETED = "deleted";
	public static final String VERSION = "version";
	public static final String DELETION_REASON = "deletionReason";

	//	TODO: определиться с допустимостью использования поля id в рамках импортов и создания записей с предопределенным id
	@Getter
	private static final Set<String> serviceInsertableFields = Set.of(_ID, /*ID,*/ HISTORY, CREATED, UPDATED, DELETED,
			DELETION_REASON, VERSION);

	@Getter
	private static final Set<String> serviceSchemeFields = Set.of(_ID, ID, HISTORY, CREATED, UPDATED, DELETED,
			DELETION_REASON, VERSION);
}
