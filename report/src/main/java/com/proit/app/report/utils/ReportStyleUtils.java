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

import lombok.experimental.UtilityClass;
import org.apache.poi.ss.usermodel.*;

import static com.proit.app.report.utils.ReportDateUtils.*;
import static com.proit.app.report.utils.ReportUtils.NUMERIC_PATTERN;

@UtilityClass
public class ReportStyleUtils
{
	public CellStyle createTitleStyle(Workbook workbook)
	{
		var font = createFont(workbook, true);
		font.setFontHeightInPoints((short) 12);

		return createStyleWithoutBorders(workbook, font, true, HorizontalAlignment.CENTER);
	}

	public CellStyle createHeaderStyle(Workbook workbook)
	{
		return createStyle(workbook, createFont(workbook, true), true, HorizontalAlignment.CENTER);
	}

	public CellStyle createHeaderStyleLeftAlignment(Workbook workbook)
	{
		return createStyle(workbook, createFont(workbook, true), true, HorizontalAlignment.LEFT);
	}

	public CellStyle createStringStyle(Workbook workbook)
	{
		return createStyle(workbook, createFont(workbook, false), true, HorizontalAlignment.LEFT);
	}

	public CellStyle createBoldStringStyleWithoutBorders(Workbook workbook)
	{
		return createStyle(workbook, createFont(workbook, true), true, HorizontalAlignment.LEFT, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE);
	}

	public CellStyle createBoldStringStyleWithoutBordersAndWithoutWrapping(Workbook workbook)
	{
		return createStyle(workbook, createFont(workbook, true), false, HorizontalAlignment.LEFT, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE);
	}

	public CellStyle createStringStyleWithoutBordersAndWithoutWrapping(Workbook workbook)
	{
		return createStyle(workbook, createFont(workbook, false), false, HorizontalAlignment.LEFT, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE);
	}

	public CellStyle createStringStyleWithoutBorders(Workbook workbook)
	{
		return createStyle(workbook, createFont(workbook, false), true, HorizontalAlignment.CENTER, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE);
	}

	public CellStyle createStringStyleWithoutBordersAndLeftAlignment(Workbook workbook)
	{
		return createStyle(workbook, createFont(workbook, false), true, HorizontalAlignment.LEFT, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE);
	}

	public CellStyle createStringStyleWithTopBorder(Workbook workbook)
	{
		return createStyle(workbook, createFont(workbook, false), true, HorizontalAlignment.CENTER, BorderStyle.NONE, BorderStyle.THIN, BorderStyle.NONE, BorderStyle.NONE);
	}

	public CellStyle createStringStyleLeftAlignmentAndBold(Workbook workbook)
	{
		return createStyle(workbook, createFont(workbook, true), true, HorizontalAlignment.LEFT);
	}

	public CellStyle createStringStyleRightAlignment(Workbook workbook)
	{
		return createStyle(workbook, createFont(workbook, false), true, HorizontalAlignment.RIGHT);
	}

	public CellStyle createStringStyleCenterAlignment(Workbook workbook)
	{
		return createStyle(workbook, createFont(workbook, false), true, HorizontalAlignment.CENTER);
	}

	public CellStyle createStringStyleCenterAlignmentBorderTopRightBottom(Workbook workbook)
	{
		return createStyle(workbook, createFont(workbook, false), true, HorizontalAlignment.CENTER, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.NONE, BorderStyle.THIN);
	}

	public CellStyle createIntegerStyle(Workbook workbook)
	{
		return createStyle(workbook, createFont(workbook, false), false, HorizontalAlignment.CENTER);
	}

	public CellStyle createNumericStyle(Workbook workbook)
	{
		return createNumericStyle(workbook, NUMERIC_PATTERN);
	}

	public CellStyle createNumericStyle(Workbook workbook, String pattern)
	{
		var format = workbook.createDataFormat();

		var style = createStyle(workbook, createFont(workbook, false), false, HorizontalAlignment.CENTER);
		style.setDataFormat(format.getFormat(pattern));

		return style;
	}

	public CellStyle createDateTimeStyle(Workbook workbook)
	{
		var helper = workbook.getCreationHelper();

		var style = createStyle(workbook, createFont(workbook, false), false, HorizontalAlignment.CENTER);
		style.setDataFormat(helper.createDataFormat().getFormat(DATE_TIME_PATTERN));

		return style;
	}

	public CellStyle createDateStyle(Workbook workbook)
	{
		var helper = workbook.getCreationHelper();

		var style = createStyle(workbook, createFont(workbook, false), false, HorizontalAlignment.CENTER);
		style.setDataFormat(helper.createDataFormat().getFormat(DATE_PATTERN));

		return style;
	}

	public CellStyle createTimeStyle(Workbook workbook)
	{
		var helper = workbook.getCreationHelper();

		var style = createStyle(workbook, createFont(workbook, false), false, HorizontalAlignment.CENTER);
		style.setDataFormat(helper.createDataFormat().getFormat(TIME_PATTERN));

		return style;
	}

	public CellStyle createYearMonthStyle(Workbook workbook)
	{
		var helper = workbook.getCreationHelper();

		var style = createStyle(workbook, createFont(workbook, false), false, HorizontalAlignment.CENTER);
		style.setDataFormat(helper.createDataFormat().getFormat(YEAR_MONTH_PATTERN));

		return style;
	}

	public CellStyle createCountStyle(Workbook workbook)
	{
		return createStyle(workbook, createFont(workbook, true), false, HorizontalAlignment.CENTER);
	}

	public CellStyle createCountHeaderStyle(Workbook workbook)
	{
		return createStyle(workbook, createFont(workbook, true), false, HorizontalAlignment.RIGHT);
	}

	private Font createFont(Workbook workbook, boolean bold)
	{
		var font = workbook.createFont();
		font.setFontName("Times New Roman");
		font.setBold(bold);
		font.setFontHeightInPoints((short) 11);

		return font;
	}

	private CellStyle createStyleWithoutBorders(Workbook workbook, Font font, boolean wrapText, HorizontalAlignment horizontalAlignment)
	{
		return createStyle(workbook, font, wrapText, horizontalAlignment, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE);
	}

	private CellStyle createStyle(Workbook workbook, Font font, boolean wrapText, HorizontalAlignment horizontalAlignment)
	{
		return createStyle(workbook, font, wrapText, horizontalAlignment, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN);
	}

	private CellStyle createStyle(Workbook workbook, Font font, boolean wrapText, HorizontalAlignment horizontalAlignment,
	                              BorderStyle bottom, BorderStyle top, BorderStyle left, BorderStyle right)
	{
		var style = workbook.createCellStyle();
		style.setWrapText(wrapText);
		style.setAlignment(horizontalAlignment);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		style.setFont(font);
		style.setBorderBottom(bottom);
		style.setBorderTop(top);
		style.setBorderLeft(left);
		style.setBorderRight(right);

		return style;
	}
}
