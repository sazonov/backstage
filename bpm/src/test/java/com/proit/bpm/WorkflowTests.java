package com.proit.bpm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proit.bpm.model.ExportedWorkflow;
import com.proit.bpm.service.ProcessService;
import com.proit.bpm.service.workflow.ClasspathWorkflowProvider;
import com.proit.bpm.service.workflow.WorkflowService;
import lombok.Cleanup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkflowTests extends AbstractTests
{
	private final String WORKFLOW_NAME = "migration";
	private final String WORKFLOW_FIRST_REVISION = WORKFLOW_NAME + "_1.0";
	private final String WORKFLOW_SECOND_REVISION = WORKFLOW_NAME + "_2.0";

	@Autowired private ObjectMapper objectMapper;

	@Autowired private WorkflowService workflowService;
	@Autowired private ProcessService processService;
	@Autowired private ClasspathWorkflowProvider classpathWorkflowProvider;

	@Value("classpath:exportedWorkflows/migration_1.0.json")
	private Resource workflow1;

	@Value("classpath:exportedWorkflows/migration_2.0.json")
	private Resource workflow2;

	@Test
	public void exportWorkflows()
	{
		var exportedWorkflows = classpathWorkflowProvider.exportWorkflows();

		assertFalse(exportedWorkflows.isEmpty());
	}

	@Order(1)
	@Test
	@SneakyThrows
	public void deployRevisions()
	{
		@Cleanup var workflowStream1 = workflow1.getInputStream();
		@Cleanup var workflowStream2 = workflow2.getInputStream();

		ExportedWorkflow workflow1 = objectMapper.readerFor(ExportedWorkflow.class).readValue(workflowStream1);
		ExportedWorkflow workflow2 = objectMapper.readerFor(ExportedWorkflow.class).readValue(workflowStream2);

		workflowService.deployWorkflow(workflow1.getId(), null, workflow1.getDefinition(), false);
		workflowService.checkWorkflows();

		workflowService.deployWorkflow(workflow2.getId(), null, workflow2.getDefinition(), false);
		workflowService.checkWorkflows();

		assertEquals(workflowService.getActualWorkflowId(WORKFLOW_NAME), WORKFLOW_SECOND_REVISION);
	}

	@Order(2)
	@Test
	public void migrateRevisions()
	{
		var process = processService.startProcess(WORKFLOW_FIRST_REVISION);
		assertNotNull(process);
		processService.sendEvent(process, "startProcess");

		processService.migrateProcess(process.getId(), WORKFLOW_SECOND_REVISION);

		process = processService.getProcess(process.getId()).orElse(null);

		assertNotNull(process);
		assertEquals(process.getWorkflowId(), WORKFLOW_SECOND_REVISION);

		processService.sendEvent(process, "nextStep");

		process = processService.getProcess(process.getId()).orElse(null);

		assertNotNull(process);
		assertTrue(process.isActive());

		processService.sendEvent(process, "nextStep2");

		process = processService.getProcess(process.getId()).orElse(null);

		assertNotNull(process);
		assertFalse(process.isActive());
	}

	@Order(3)
	@Test
	void initializationTest()
	{
		workflowService.initialize();
	}
}
