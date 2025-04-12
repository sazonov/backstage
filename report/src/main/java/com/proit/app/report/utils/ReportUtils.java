/*
 *    Copyright 2019-2024 the original author or authors.
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

package com.proit.app.report.utils;

import com.proit.app.report.model.filter.ReportFilter;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;

import static com.proit.app.report.utils.ReportDateUtils.LOCAL_DATE_FORMATTER;

@UtilityClass
public class ReportUtils
{
	public static final String HYPHEN = " — ";
	public static final String NUMERIC_PATTERN = "#,##0.00";

	private static final short EXCEL_COLUMN_WIDTH_FACTOR = 256;
	private static final int UNIT_OFFSET_LENGTH = 7;
	private static final int[] UNIT_OFFSET_MAP = new int[]{0, 36, 73, 109, 146, 182, 219};

	public String getReportFileName(ReportFilter filter)
	{
		var from = filter.getFrom() != null ? filter.getFrom() : LocalDate.now();
		var to = filter.getTo() != null ? filter.getTo() : from;

		var result = filter.getReportType().getTitle() + " за " + LOCAL_DATE_FORMATTER.format(from);

		if (!from.isEqual(to))
		{
			result += HYPHEN + LOCAL_DATE_FORMATTER.format(to);
		}

		return result + filter.getReportType().getReportFileType().getExtension();
	}
}
