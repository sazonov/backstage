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

package com.proit.app.report.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public enum ReportFileType
{
	XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
	CSV("text/csv", ".csv"),
	JSON("application/json", ".json"),
	DOCX( "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx");

	private String mimeType;
	private String extension;
}
