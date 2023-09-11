package com.proit.app.report.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExampleReportType implements ReportType
{
	EXAMPLE_XLS_1("Пример XLS отчета 1", ReportFileType.XLSX);

	private final String title;
	private final ReportFileType reportFileType;
}
