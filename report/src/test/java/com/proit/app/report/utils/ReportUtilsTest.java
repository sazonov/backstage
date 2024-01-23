package com.proit.app.report.utils;

import com.proit.app.report.AbstractTest;
import com.proit.app.report.model.ExampleReportType;
import com.proit.app.report.model.filter.SimpleReportFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReportUtilsTest extends AbstractTest
{
	private static final  String SHORT_TEMPLATE = "%s за %s%s";
	private static final  String FULL_TEMPLATE = "%s за %s%s%s%s";
	private static final LocalDate FROM = LocalDate.of(2000, 1, 1);
	private static final LocalDate TO = LocalDate.of(2000, 12, 31);

	@Test
	public void checkFileNameMethodFullFilterTest()
	{
		var repotType = ExampleReportType.EXAMPLE_1;

		var filter = SimpleReportFilter.builder()
				.reportType(repotType)
				.from(FROM)
				.to(TO)
				.build();

		var expectedFileName = FULL_TEMPLATE.formatted(
				repotType.getTitle(),
				ReportDateUtils.LOCAL_DATE_FORMATTER.format(FROM),
				ReportUtils.HYPHEN,
				ReportDateUtils.LOCAL_DATE_FORMATTER.format(TO),
				repotType.getReportFileType().getExtension());

		var fileName = ReportUtils.getReportFileName(filter);

		assertEquals(expectedFileName, fileName);
	}

	@Test
	public void checkFileNameMethodShortFilterTest()
	{
		var repotType = ExampleReportType.EXAMPLE_1;

		var filter = SimpleReportFilter.builder()
				.reportType(repotType)
				.from(FROM)
				.to(FROM)
				.build();

		var expectedFileName = SHORT_TEMPLATE.formatted(
				repotType.getTitle(),
				ReportDateUtils.LOCAL_DATE_FORMATTER.format(FROM),
				repotType.getReportFileType().getExtension());

		var fileName = ReportUtils.getReportFileName(filter);

		assertEquals(expectedFileName, fileName);
	}
}
