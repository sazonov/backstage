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

package com.proit.app.report.utils;

import com.proit.app.report.model.filter.ReportFilter;
import com.proit.app.report.service.builder.SheetBuilder;
import lombok.experimental.UtilityClass;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.util.CellRangeAddress;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.proit.app.report.utils.ReportDateUtils.LOCAL_DATE_FORMATTER;
import static com.proit.app.report.utils.ReportDateUtils.LOCAL_DATE_TIME_FORMATTER;

@UtilityClass
public class ReportUtils
{
	public static final String HYPHEN = " — ";
	public static final String NUMERIC_PATTERN = "#,##0.00";

	private static final short EXCEL_COLUMN_WIDTH_FACTOR = 256;
	private static final int UNIT_OFFSET_LENGTH = 7;
	private static final int[] UNIT_OFFSET_MAP = new int[]{0, 36, 73, 109, 146, 182, 219};

	public short pixelToWidthUnits(int pxs)
	{
		var widthUnits = (short) (EXCEL_COLUMN_WIDTH_FACTOR * (pxs / UNIT_OFFSET_LENGTH));
		widthUnits += UNIT_OFFSET_MAP[(pxs % UNIT_OFFSET_LENGTH)];

		return widthUnits;
	}

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

	public void addHeaderDefault(SheetBuilder builder, ReportFilter filter)
	{
		String dateRange = String.join(HYPHEN, LOCAL_DATE_FORMATTER.format(filter.getFrom()), LOCAL_DATE_FORMATTER.format(filter.getTo()));

		builder.getSheet()
				.addMergedRegion(new CellRangeAddress(builder.getSheet().getLastRowNum(), builder.getSheet().getLastRowNum(), 0, 1));
		builder.addHeader(filter.getReportType().getTitle(), 150)
				.createRow();
		builder.addHeader("Период:", 150)
				.addStringCell(dateRange)
				.createRow();
	}

	public void addFooterDefault(SheetBuilder builder, String title)
	{
		builder.createRow()
				.addStringCell(title);
		builder.addMergedRegionWithBorders(
				builder.getSheet().getLastRowNum(),
				builder.getSheet().getLastRowNum(), 0, 3);
		builder.createRow().addStringCellWithoutBorders(0, "Дата и время формирования отчета: ");
		builder.addMergedRegionWithBorders(
				builder.getSheet().getLastRowNum(),
				builder.getSheet().getLastRowNum(), 0, 2, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.NONE);
		builder.addStringCellCenterAlignmentBorderTopRightBottom(3, LOCAL_DATE_TIME_FORMATTER.format(LocalDateTime.now()));
	}
}
