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

package com.proit.app.report.service.generator;

import com.proit.app.report.exception.ReportGenerateException;
import com.proit.app.report.model.filter.ReportFilter;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class AbstractXlsxReportGenerator<T extends ReportFilter> extends AbstractReportGenerator<T>
{
	protected byte[] saveWorkbook(Workbook workbook)
	{
		try (var outputStream = new ByteArrayOutputStream())
		{
			workbook.write(outputStream);

			return outputStream.toByteArray();
		}
		catch (IOException e)
		{
			throw new ReportGenerateException(e);
		}
	}

	protected Workbook loadWorkbook(String path)
	{
		try (var inputStream = getClass().getResourceAsStream(path))
		{
			return new XSSFWorkbook(inputStream);
		}
		catch (IOException e)
		{
			throw new ReportGenerateException(e);
		}
	}
}
