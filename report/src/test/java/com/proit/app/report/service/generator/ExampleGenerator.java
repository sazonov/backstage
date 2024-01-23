package com.proit.app.report.service.generator;

import com.proit.app.report.model.filter.SimpleReportFilter;
import org.springframework.stereotype.Service;

@Service
public class ExampleGenerator extends AbstractReportGenerator<SimpleReportFilter>
{

	@Override
	public byte[] generate(SimpleReportFilter filter)
	{
		return new byte[0];
	}
}
