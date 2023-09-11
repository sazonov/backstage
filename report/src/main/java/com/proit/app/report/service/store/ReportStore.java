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

package com.proit.app.report.service.store;

import com.proit.app.report.model.report.Report;

public interface ReportStore
{
	Report create(String fileName, String mimeType, byte[] data);

	Report getById(String id);

	String getIdByLinkedObject(String objectKey, String objectId);

	void linkReportWithObject(String id, String objectKey, String objectId);

	Report update(String id, String fileName, String mimeType, byte[] data);

	void release(String bindingKey, String objectId);
}
