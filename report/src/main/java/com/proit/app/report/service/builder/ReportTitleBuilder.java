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

package com.proit.app.report.service.builder;

import com.proit.app.report.utils.ReportStyleUtils;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;

public class ReportTitleBuilder
{
	private ReportTitleBuilder(SXSSFSheet sheet, int columnsCount, String title)
	{
		sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columnsCount - 1));

		var cell = sheet.createRow(0).createCell(0);
		cell.setCellStyle(ReportStyleUtils.createTitleStyle(sheet.getWorkbook()));
		cell.setCellValue(title);
	}

	public static ReportTitleBuilder create(SXSSFSheet sheet, int columnsCount, String title)
	{
		return new ReportTitleBuilder(sheet, columnsCount, title);
	}
}
