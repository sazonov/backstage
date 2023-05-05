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

package com.proit.app.service.export;

import com.proit.app.constant.ExportedDictFormat;
import com.proit.app.exception.AppException;
import com.proit.app.model.api.ApiStatusCodeImpl;
import com.proit.app.model.export.ExportedResource;
import com.proit.app.service.DictDataService;
import com.proit.app.service.DictPermissionService;
import com.proit.app.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.List;

import static com.proit.app.util.ExportUtils.generateFilename;

@Service
@RequiredArgsConstructor
public class DictExportService
{
	private final ExportCsvService exportCsvService;
	private final ExportSqlService exportSqlService;
	private final ExportJsonService exportJsonService;

	private final DictDataService dictDataService;
	private final DictPermissionService dictPermissionService;

	public ExportedResource exportToResource(String dictId, ExportedDictFormat format, List<String> itemIds)
	{
		return exportToResource(dictId, format, itemIds, SecurityUtils.getCurrentUserId());
	}

	public ExportedResource exportToResource(String dictId, ExportedDictFormat format, List<String> itemIds, String userId)
	{
		dictPermissionService.checkViewPermission(dictId, userId);

		var items = (itemIds != null && !itemIds.isEmpty())
				? dictDataService.getByIds(dictId, itemIds, userId)
				: dictDataService.getByFilter(dictId, List.of(), null, Pageable.unpaged(), userId).getContent();

		byte[] exportedData = getExportService(format).export(dictId, items, userId);

		return ExportedResource.builder()
				.resource(new InputStreamResource(new ByteArrayInputStream(exportedData)))
				.filename(generateFilename(dictId, format, itemIds))
				.build();
	}

	private ExportService getExportService(ExportedDictFormat format)
	{
		return switch (format)
				{
					case SQL -> exportSqlService;
					case CSV -> exportCsvService;
					case JSON -> exportJsonService;

					default -> throw new AppException(ApiStatusCodeImpl.ILLEGAL_DATA_FORMAT);
				};
	}
}
