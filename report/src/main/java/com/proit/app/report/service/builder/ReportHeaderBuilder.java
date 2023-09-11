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
import com.proit.app.report.utils.ReportUtils;
import lombok.Getter;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;

import java.util.stream.IntStream;

public class ReportHeaderBuilder
{
	private static final float UNITS_TO_PIXEL = 0.75f;
	private static final int DEFAULT_HEIGHT_IN_PIXELS = 20;

	private SXSSFSheet sheet;

	@Getter
	private int currentRowNumber;
	@Getter
	private int currentColumnNumber;

	private SXSSFRow currentRow;

	private CellStyle headerStyle;
	private CellStyle numbersRowStyle;

	private ReportHeaderBuilder(SXSSFSheet sheet, int currentRowNumber)
	{
		this.sheet = sheet;
		this.currentRowNumber = currentRowNumber;

		currentRow = sheet.createRow(currentRowNumber);

		headerStyle = ReportStyleUtils.createHeaderStyle(sheet.getWorkbook());
		numbersRowStyle = ReportStyleUtils.createIntegerStyle(sheet.getWorkbook());
	}

	public static ReportHeaderBuilder create(SXSSFSheet sheet, int currentRowNumber)
	{
		return new ReportHeaderBuilder(sheet, currentRowNumber);
	}

	public ReportHeaderBuilder addColumn(String title, Merge merge)
	{
		return addColumn(title, 0, merge);
	}

	public ReportHeaderBuilder addColumn(String title, int width, Merge merge)
	{
		if (merge.hasMerge())
		{
			sheet.addMergedRegion(new CellRangeAddress(
					currentRowNumber, currentRowNumber + merge.getVertical() - 1,
					currentColumnNumber, currentColumnNumber + merge.getHorizontal() - 1));
		}

		var cell = currentRow.createCell(currentColumnNumber++);
		cell.setCellStyle(headerStyle);
		cell.setCellValue(title);

		if (width > 0)
		{
			sheet.setColumnWidth(currentColumnNumber - 1, ReportUtils.pixelToWidthUnits(width));
		}

		if (merge.hasHorizontalMerge())
		{
			IntStream.range(1, merge.getHorizontal())
					.forEach(it -> currentRow.createCell(currentColumnNumber++)
							.setCellStyle(headerStyle));
		}

		return this;
	}

	public ReportHeaderBuilder skipColumns(int count)
	{
		for (int i = 0; i < count; i++)
		{
			currentRow.createCell(currentColumnNumber++)
					.setCellStyle(headerStyle);
		}

		return this;
	}

	public ReportHeaderBuilder addRow()
	{
		return addRow(DEFAULT_HEIGHT_IN_PIXELS);
	}

	public ReportHeaderBuilder addRow(int height)
	{
		currentRow = sheet.createRow(++currentRowNumber);
		currentRow.setHeightInPoints(height * UNITS_TO_PIXEL);

		currentColumnNumber = 0;

		return this;
	}

	public ReportHeaderBuilder addNumbersRow()
	{
		var columnsCount = currentColumnNumber;

		addRow();

		for (int i = 0; i < columnsCount; i++)
		{
			var cell = currentRow.createCell(currentColumnNumber++);
			cell.setCellStyle(numbersRowStyle);
			cell.setCellValue(Integer.toString(i + 1));
		}

		return this;
	}

	public ReportHeaderBuilder createFreeze(int column)
	{
		sheet.createFreezePane(column, currentRowNumber + 1);

		return this;
	}

	public ReportHeaderBuilder setHeightInPoints(float height)
	{
		currentRow.setHeightInPoints(height);

		return this;
	}
}
