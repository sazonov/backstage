package com.proit.app.report.model;

import com.proit.app.report.model.filter.EmptyReportFilter;
import com.proit.app.report.model.filter.ReportFilter;

public interface NoFilterReportType extends ReportType
{
	@Override
	default Class<? extends ReportFilter> getFilterType()
	{
		return EmptyReportFilter.class;
	}
}
