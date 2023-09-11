package com.proit.app.report.service.generator;

import com.proit.app.report.model.ExampleReportType;
import com.proit.app.report.model.ReportType;
import com.proit.app.report.model.filter.SimpleReportFilter;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExampleXls1Generator extends AbstractXlsxReportGenerator<SimpleReportFilter>
{
	@Override
	public ReportType getReportType()
	{
		return ExampleReportType.EXAMPLE_XLS_1;
	}

	@Override
	public byte[] generate(SimpleReportFilter filter)
	{
		return saveWorkbook(new SXSSFWorkbook());
	}
}
