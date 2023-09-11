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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static com.proit.app.report.utils.ReportDateUtils.*;

public class SheetBuilder
{
	private SXSSFSheet sheet;
	private CellStyle headerStyle;
	private CellStyle headerStyleLeftAlignment;
	private CellStyle stringStyle;
	private CellStyle stringStyleWithoutBorders;
	private CellStyle stringStyleCenterAlignment;
	private CellStyle stringStyleWithTopBorder;
	private CellStyle stringStyleCenterAlignmentBorderTopRightBottom;
	private CellStyle boldStringStyleWithoutBorders;
	private CellStyle integerStyle;
	private CellStyle numericStyle;
	private CellStyle dateStyle;
	private CellStyle dateTimeStyle;
	private CellStyle timeStyle;
	private CellStyle countStyle;
	private CellStyle countHeaderStyle;
	private CellStyle yearMonthStyle;

	@Getter
	private int currentRowNumber = 0;
	private Row currentRow;

	@Getter
	private int currentColumnNumber = 0;

	private Set<Integer> headerIndexes = new HashSet<>();

	private SheetBuilder(SXSSFWorkbook workbook, String title, boolean skipFirstRow)
	{
		sheet = workbook.createSheet(title);
		sheet.setRandomAccessWindowSize(1_000);

		currentRow = sheet.createRow(currentRowNumber++);

		if (skipFirstRow)
		{
			currentRow = sheet.createRow(currentRowNumber++);
		}

		headerStyle = ReportStyleUtils.createHeaderStyle(workbook);
		headerStyleLeftAlignment = ReportStyleUtils.createHeaderStyleLeftAlignment(workbook);
		stringStyle = ReportStyleUtils.createStringStyle(workbook);
		boldStringStyleWithoutBorders = ReportStyleUtils.createBoldStringStyleWithoutBorders(workbook);
		stringStyleWithoutBorders = ReportStyleUtils.createStringStyleWithoutBordersAndLeftAlignment(workbook);
		stringStyleWithTopBorder = ReportStyleUtils.createStringStyleWithTopBorder(workbook);
		stringStyleCenterAlignment = ReportStyleUtils.createStringStyleCenterAlignment(workbook);
		stringStyleCenterAlignmentBorderTopRightBottom = ReportStyleUtils.createStringStyleCenterAlignmentBorderTopRightBottom(workbook);
		integerStyle = ReportStyleUtils.createIntegerStyle(workbook);
		numericStyle = ReportStyleUtils.createNumericStyle(workbook);
		dateStyle = ReportStyleUtils.createDateStyle(workbook);
		dateTimeStyle = ReportStyleUtils.createDateTimeStyle(workbook);
		timeStyle = ReportStyleUtils.createTimeStyle(workbook);
		countStyle = ReportStyleUtils.createCountStyle(workbook);
		countHeaderStyle = ReportStyleUtils.createCountHeaderStyle(workbook);
		yearMonthStyle = ReportStyleUtils.createYearMonthStyle(workbook);
	}

	private SheetBuilder(SXSSFSheet sheet, int currentRowNumber)
	{
		this.sheet = sheet;
		this.currentRowNumber = currentRowNumber;

		stringStyle = ReportStyleUtils.createStringStyle(sheet.getWorkbook());
		boldStringStyleWithoutBorders = ReportStyleUtils.createBoldStringStyleWithoutBorders(sheet.getWorkbook());
		stringStyleCenterAlignment = ReportStyleUtils.createStringStyleCenterAlignment(sheet.getWorkbook());
		integerStyle = ReportStyleUtils.createIntegerStyle(sheet.getWorkbook());
		numericStyle = ReportStyleUtils.createNumericStyle(sheet.getWorkbook());
		dateStyle = ReportStyleUtils.createDateStyle(sheet.getWorkbook());
		dateTimeStyle = ReportStyleUtils.createDateTimeStyle(sheet.getWorkbook());
		timeStyle = ReportStyleUtils.createTimeStyle(sheet.getWorkbook());
		countStyle = ReportStyleUtils.createCountStyle(sheet.getWorkbook());
		countHeaderStyle = ReportStyleUtils.createCountHeaderStyle(sheet.getWorkbook());
	}

	public static SheetBuilder create(SXSSFWorkbook workbook, String title)
	{
		return new SheetBuilder(workbook, title, true);
	}

	public static SheetBuilder create(SXSSFWorkbook workbook, String title, boolean skipFirstRow)
	{
		return new SheetBuilder(workbook, title, skipFirstRow);
	}

	public static SheetBuilder create(SXSSFSheet sheet, int currentRowNumber)
	{
		return new SheetBuilder(sheet, currentRowNumber);
	}

	public SheetBuilder createRow()
	{
		return createRow(true);
	}

	public SheetBuilder createRow(boolean fixedHeight)
	{
		currentRow = sheet.createRow(currentRowNumber++);
		currentColumnNumber = 0;

		if (fixedHeight)
		{
			currentRow.setHeightInPoints(15);
		}

		return this;
	}

	public SheetBuilder createRow(int height)
	{
		currentRow = sheet.createRow(currentRowNumber++);
		currentColumnNumber = 0;

		currentRow.setHeightInPoints(height);

		return this;
	}

	public SheetBuilder setCurrentRow(int index)
	{
		currentRow = sheet.getRow(index) == null ? sheet.createRow(index) : sheet.getRow(index);
		currentRowNumber = index + 1;
		currentColumnNumber = index;

		return this;
	}

	public SheetBuilder setCurrentRowNum(int index)
	{
		currentRow = sheet.getRow(index) == null ? sheet.createRow(index) : sheet.getRow(index);
		currentRowNumber = index + 1;

		return this;
	}

	public SheetBuilder setCurrentColumnNumber(int index)
	{
		currentColumnNumber = index;

		return this;
	}

	public SheetBuilder addTitle(String title)
	{
		addTitle(title, Math.max(headerIndexes.size() - 1, 1));

		return this;
	}

	public SheetBuilder addTitle(String title, int mergeWidth)
	{
		sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, mergeWidth));

		currentRow = sheet.getRow(0);

		var style = ReportStyleUtils.createTitleStyle(sheet.getWorkbook());

		var cell = currentRow.createCell(0);
		cell.setCellStyle(style);
		cell.setCellValue(title);

		return this;
	}

	public SheetBuilder setHeightInPoints(float height)
	{
		currentRow.setHeightInPoints(height);

		return this;
	}

	public SheetBuilder addMergedRegionWithBorders(int firstRow, int lastRow, int firstColumn, int lastColumn)
	{
		return addMergedRegionWithBorders(firstRow, lastRow, firstColumn, lastColumn, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN);
	}

	public SheetBuilder addMergedRegionWithBorders(int firstRow, int lastRow, int firstColumn, int lastColumn,
	                                               BorderStyle top, BorderStyle bottom, BorderStyle left, BorderStyle right)
	{
		var region = new CellRangeAddress(firstRow, lastRow, firstColumn, lastColumn);
		sheet.addMergedRegion(region);
		RegionUtil.setBorderTop(top, region, sheet);
		RegionUtil.setBorderBottom(bottom, region, sheet);
		RegionUtil.setBorderLeft(left, region, sheet);
		RegionUtil.setBorderRight(right, region, sheet);

		return this;
	}

	public SheetBuilder addMergedRegionWithTopBorders(int firstRow, int lastRow, int firstColumn, int lastColumn)
	{
		return addMergedRegionWithBorders(firstRow, lastRow, firstColumn, lastColumn, BorderStyle.THIN, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE);
	}

	public SheetBuilder addMergedRegion(int firstRow, int lastRow, int firstColumn, int lastColumn)
	{
		sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstColumn, lastColumn));

		return this;
	}

	public SheetBuilder addHeaderLeftAlignment(String header, int width)
	{
		return addHeader(currentColumnNumber++, header, width, headerStyleLeftAlignment);
	}

	public SheetBuilder addHeader(String header, int width)
	{
		return addHeader(currentColumnNumber++, header, width);
	}

	public SheetBuilder addHeader(int index, String header, int width)
	{
		return addHeader(index, header, width, headerStyle);
	}

	public SheetBuilder addHeader(String header, int width, CellStyle style)
	{
		sheet.setColumnWidth(currentColumnNumber, ReportUtils.pixelToWidthUnits(width));

		var cell = currentRow.createCell(currentColumnNumber);
		cell.setCellStyle(style);
		cell.setCellValue(header);

		headerIndexes.add(currentColumnNumber);
		currentColumnNumber++;

		return this;
	}

	public SheetBuilder addHeader(int index, String header, int width, CellStyle style)
	{
		sheet.setColumnWidth(index, ReportUtils.pixelToWidthUnits(width));

		var cell = currentRow.createCell(index);
		cell.setCellStyle(style);
		cell.setCellValue(header);

		headerIndexes.add(index);

		return this;
	}

	public SheetBuilder addNumberRow()
	{
		currentRow = sheet.createRow(currentRowNumber++);

		IntStream.range(1, headerIndexes.size() + 1)
				.forEach(this::addIntegerCell);

		return this;
	}

	public SheetBuilder addStringCell(int index, Object value)
	{
		return addStringCell(index, value, stringStyle);
	}

	public SheetBuilder addBoldStringCellWithoutBorders(int index, Object value)
	{
		return addStringCell(index, value, boldStringStyleWithoutBorders);
	}

	public SheetBuilder addStringCell(Object value)
	{
		return addStringCell(currentColumnNumber++, value);
	}

	public SheetBuilder addStringCellWrapText(Object value)
	{
		var style = ReportStyleUtils.createStringStyle(sheet.getWorkbook());
		style.setWrapText(true);

		return addStringCell(currentColumnNumber++, value, style);
	}

	public SheetBuilder addStringCellCenterAlignment(Object value)
	{
		return addStringCell(currentColumnNumber++, value, stringStyleCenterAlignment);
	}

	public SheetBuilder addStringCellCenterAlignment(int index, Object value)
	{
		return addStringCell(index, value, stringStyleCenterAlignment);
	}

	public SheetBuilder addStringCellCenterAlignmentBorderTopRightBottom(int index, Object value)
	{
		return addStringCell(index, value, stringStyleCenterAlignmentBorderTopRightBottom);
	}

	public SheetBuilder addStringCellWithoutBorders(int index, Object value)
	{
		return addStringCell(index, value, stringStyleWithoutBorders);
	}

	public SheetBuilder addStringCellWithTopBorder(int index, Object value)
	{
		return addStringCell(index, value, stringStyleWithTopBorder);
	}

	public SheetBuilder addIntegerCell(int index, Object value)
	{
		var cell = currentRow.createCell(index);
		cell.setCellStyle(integerStyle);

		if (value != null)
		{
			cell.setCellValue((Integer) value);
		}

		return this;
	}

	public SheetBuilder addIntegerCell(Object value)
	{
		return addIntegerCell(currentColumnNumber++, value);
	}

	public SheetBuilder addLongCell(int index, Object value)
	{
		var cell = currentRow.createCell(index);
		cell.setCellStyle(integerStyle);

		if (value != null)
		{
			cell.setCellValue((Long) value);
		}

		return this;
	}

	public SheetBuilder addLongCell(Object value)
	{
		return addLongCell(currentColumnNumber++, value);
	}

	public SheetBuilder addDoubleCell(int index, Object value)
	{
		return addDoubleCell(index, value, numericStyle);
	}

	public SheetBuilder addDoubleCell(int index, Object value, CellStyle cellStyle)
	{
		var cell = currentRow.createCell(index);
		cell.setCellStyle(cellStyle);

		if (value != null)
		{
			cell.setCellValue((Double) value);
		}

		return this;
	}

	public SheetBuilder addDoubleCell(Object value, String pattern)
	{
		return addDoubleCell(currentColumnNumber++, value, ReportStyleUtils.createNumericStyle(sheet.getWorkbook(), pattern));
	}

	public SheetBuilder addDoubleCell(Object value)
	{
		return addDoubleCell(currentColumnNumber++, value, numericStyle);
	}

	public SheetBuilder addDoubleCell(Object value, CellStyle cellStyle)
	{
		return addDoubleCell(currentColumnNumber++, value, cellStyle);
	}

	public SheetBuilder addDateTimeCell(int index, Object value)
	{
		var cell = currentRow.createCell(index);
		cell.setCellStyle(dateTimeStyle);

		if (value != null)
		{
			cell.setCellValue(DATE_TIME_FORMATTER.format((Date) value));
		}

		return this;
	}

	public SheetBuilder addDateTimeCell(Object value)
	{
		return addDateTimeCell(currentColumnNumber++, value);
	}

	public SheetBuilder addDateCell(int index, Object value)
	{
		var cell = currentRow.createCell(index);
		cell.setCellStyle(dateStyle);

		if (value != null)
		{
			cell.setCellValue(DateUtils.truncate((Date) value, Calendar.DAY_OF_MONTH));
		}

		return this;
	}

	public SheetBuilder addDateCell(Object value)
	{
		return addDateCell(currentColumnNumber++, value);
	}

	public SheetBuilder addTimeCell(int index, Object value, String timePattern)
	{
		var cell = currentRow.createCell(index);
		cell.setCellStyle(timeStyle);

		if (value != null)
		{
			cell.setCellValue(DateFormatUtils.format((Date) value, timePattern));
		}

		return this;
	}

	public SheetBuilder addTimeCell(int index, Object value)
	{
		var cell = currentRow.createCell(index);
		cell.setCellStyle(timeStyle);

		if (value instanceof Date date)
		{
			cell.setCellValue(TIME_FORMATTER.format(date));
		}

		if (value instanceof LocalTime time)
		{
			cell.setCellValue(LOCAL_TIME_FORMATTER.format(time));
		}

		return this;
	}

	public SheetBuilder addTimeCell(Object value)
	{
		return addTimeCell(currentColumnNumber++, value);
	}

	public SheetBuilder addYearMonthCell(Object value)
	{
		var cell = currentRow.createCell(currentColumnNumber++);
		cell.setCellStyle(yearMonthStyle);

		if (value != null)
		{
			cell.setCellValue(YEAR_MONTH_FORMATTER.format((Date) value));
		}

		return this;
	}

	public SheetBuilder addLocalDateCell(int index, Object value)
	{
		var cell = currentRow.createCell(index);
		cell.setCellStyle(dateStyle);

		if (value != null)
		{
			cell.setCellValue((LocalDate) value);
		}

		return this;
	}

	public SheetBuilder addLocalDateCell(Object value)
	{
		return addLocalDateCell(currentColumnNumber++, value);
	}

	public SheetBuilder addLocalDateTimeCell(int index, Object value)
	{
		var cell = currentRow.createCell(index);
		cell.setCellStyle(dateTimeStyle);

		if (value != null)
		{
			cell.setCellValue(LOCAL_DATE_TIME_FORMATTER.format((LocalDateTime) value));
		}

		return this;
	}

	public SheetBuilder addLocalDateTimeCell(Object value)
	{
		return addLocalDateTimeCell(currentColumnNumber++, value);
	}

	public SheetBuilder addEmptyCell(int index)
	{
		var cell = currentRow.createCell(index);
		cell.setCellStyle(stringStyle);

		return this;
	}

	public SheetBuilder addEmptyCell()
	{
		return addEmptyCell(currentColumnNumber++);
	}

	public SheetBuilder addCountHeader(String header)
	{
		var cell = currentRow.createCell(currentColumnNumber++);
		cell.setCellStyle(countHeaderStyle);
		cell.setCellValue(header);

		return this;
	}

	public SheetBuilder addCountCell(Integer count)
	{
		var cell = currentRow.createCell(currentColumnNumber++);
		cell.setCellStyle(countStyle);
		cell.setCellValue(count);

		return this;
	}

	private SheetBuilder addStringCell(int index, Object value, CellStyle style)
	{
		var cell = currentRow.createCell(index);
		cell.setCellStyle(style);

		if (value != null)
		{
			cell.setCellValue((String) value);
		}

		return this;
	}

	public SheetBuilder addStringCell(Object value, CellStyle style)
	{
		return addStringCell(currentColumnNumber++, value, style);
	}

	public SheetBuilder addEmptyCells(int from, int to)
	{
		IntStream.range(from, to + 1)
				.forEach(this::addEmptyCell);

		currentColumnNumber += to;

		return this;
	}

	public SheetBuilder addFormulaCell(Object formula, int start, int end)
	{
		var cell = currentRow.createCell(currentColumnNumber++);
		cell.setCellStyle(headerStyle);

		if (formula != null)
		{
			cell.setCellFormula((String.format((String) formula, getColumnName(currentColumnNumber - 1), start, getColumnName(currentColumnNumber - 1), end)));
		}

		return this;
	}

	public SheetBuilder addFormulaCell(String formula, int length)
	{
		var cell = currentRow.createCell(currentColumnNumber++);
		cell.setCellStyle(headerStyle);

		if (StringUtils.isNotBlank(formula))
		{
			var cellFormula = formula.formatted(getColumnName(currentColumnNumber), currentRowNumber,
					getColumnName(currentColumnNumber + length), currentRowNumber);

			cell.setCellFormula(cellFormula);
		}

		return this;
	}

	public SheetBuilder addBooleanField(boolean value)
	{
		return addStringCell(value ? "Да" : "Нет");
	}

	public SheetBuilder addBooleanFieldCenterAlignment(boolean value)
	{
		return addStringCell(value ? "Да" : "Нет", stringStyleCenterAlignment);
	}

	private String getColumnName(int index)
	{
		var s = new StringBuilder();

		if (index >= 26)
		{
			s.insert(0, (char) ('A' + index % 26));
			index = index / 26 - 1;
		}

		s.insert(0, (char) ('A' + index));

		return s.toString();
	}

	public SXSSFSheet getSheet()
	{
		return sheet;
	}
}
