package com.proit.app.report.service;

import com.proit.app.report.AbstractTest;
import com.proit.app.report.model.ExampleReportType;
import com.proit.app.report.model.filter.SimpleReportFilter;
import com.proit.app.report.utils.ReportUtils;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ServiceGeneratorTest extends AbstractTest
{
	@Autowired private GeneratorLocator generatorLocator;

	@Test
	public void exampleXls1GeneratorFileNameFullFilterTest()
	{
		var filter = SimpleReportFilter.builder()
				.reportType(ExampleReportType.EXAMPLE_1)
				.from(FROM)
				.to(TO)
				.build();

		var expectedFileName = ReportUtils.getReportFileName(filter);

		var fileName = generatorLocator.getGenerator(filter.getReportType()).generateReportFileName(filter);

		assertEquals(expectedFileName, fileName);
	}

	@Test
	public void exampleXls1GeneratorTest() throws IOException
	{
		var filter = SimpleReportFilter.builder()
				.reportType(ExampleReportType.EXAMPLE_1)
				.from(FROM)
				.to(TO)
				.build();

		var workbook = new SXSSFWorkbook();

		var outputStream = new ByteArrayOutputStream();
		workbook.write(outputStream);
		byte[] expectedData = outputStream.toByteArray();

		byte[] data = generatorLocator.getGenerator(filter.getReportType()).generate(filter);

//		assertEquals(expectedData.length, data.length);
	}
}
