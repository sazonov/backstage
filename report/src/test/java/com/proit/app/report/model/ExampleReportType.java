package com.proit.app.report.model;

import com.proit.app.report.model.filter.ReportFilter;
import com.proit.app.report.model.filter.SimpleReportFilter;
import com.proit.app.report.service.generator.ExampleGenerator;
import com.proit.app.report.service.generator.ReportGenerator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExampleReportType implements ReportType
{
	EXAMPLE_1("Пример отчета 1", ReportFileType.XLSX, ExampleGenerator.class, SimpleReportFilter.class);

	private final String title;
	private final ReportFileType reportFileType;
	private final Class<? extends ReportGenerator<? extends ReportFilter>> generatorType;
	private final Class<? extends ReportFilter> filterType;
}
