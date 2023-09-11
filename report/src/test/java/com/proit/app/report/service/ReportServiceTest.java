package com.proit.app.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proit.app.report.AbstractTest;
import com.proit.app.report.model.ExampleReportType;
import com.proit.app.report.model.ReportMessage;
import com.proit.app.report.model.filter.SimpleReportFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReportServiceTest extends AbstractTest
{
	private static final String USER_ID = "system";
	private static final String TASK_ID = "taskId";
	private static final String MIME_TYPE = "mimeType";

	@Autowired private ReportService reportService;

	@Autowired private ObjectMapper objectMapper;

	@Test
	public void generateExampleXlsCorrectTest()
	{
		var filter = SimpleReportFilter.builder()
				.reportType(ExampleReportType.EXAMPLE_XLS_1)
				.from(FROM)
				.to(TO)
				.build();

		assertDoesNotThrow(() -> reportService.generate(filter, USER_ID));
	}

	@Test
	public void serializeCorrectReportMessageTest()
	{
		var filter = SimpleReportFilter.builder()
				.reportType(ExampleReportType.EXAMPLE_XLS_1)
				.from(FROM)
				.to(TO)
				.build();

		var reportMessage = new ReportMessage();
		reportMessage.setReportFilter(filter);
		reportMessage.setMimeType(MIME_TYPE);
		reportMessage.setTaskId(TASK_ID);
		reportMessage.setUserId(USER_ID);

		assertDoesNotThrow(() -> objectMapper.writeValueAsString(reportMessage));
	}

	@Test
	public void deserializeCorrectReportMessageTest()
	{
		var correctSerializedReportMessage = "{\"reportFilter\":{\"@c\":\"com.proit.app.report.model.filter.SimpleReportFilter\",\"reportType\":[\"com.proit.app.report.model.ExampleReportType\",\"EXAMPLE_XLS_1\"]}}";

		assertDoesNotThrow(() -> objectMapper.readValue(correctSerializedReportMessage, ReportMessage.class));
	}

	@Test
	public void DeserializeWrongReportMessageTest()
	{
		var wrongSerializedReportMessage = "{\"reportFilter\":{\"reportType\":\"EXAMPLE_XLS_1\"}}";

		assertThrows(JsonProcessingException.class, () -> objectMapper.readValue(wrongSerializedReportMessage, ReportMessage.class));
	}
}
