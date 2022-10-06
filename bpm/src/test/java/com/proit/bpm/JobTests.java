package com.proit.bpm;

import com.proit.app.service.job.JobManager;
import com.proit.bpm.repository.ProcessRepository;
import com.proit.bpm.repository.TaskRepository;
import com.proit.bpm.service.job.ArchiveWorkflowsJob;
import com.proit.bpm.service.job.AutoMigrateProcessesJob;
import com.proit.bpm.service.job.DeleteInactiveProcessesJob;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Order(1000)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JobTests extends AbstractTests
{
	@Autowired private TaskRepository taskRepository;
	@Autowired private ProcessRepository processRepository;

	@Autowired private JobManager jobManager;

	@Test
	public void runDeleteInactiveProcessesJob()
	{
		var taskCount = taskRepository.count();
		var processCount = processRepository.count();

		jobManager.executeJobAndWait(DeleteInactiveProcessesJob.class);

		assertTrue(taskRepository.count() < taskCount);
		assertTrue(processRepository.count() < processCount);
	}

	@Test
	public void runArchiveWorkflowsJob()
	{
		jobManager.executeJobAndWait(ArchiveWorkflowsJob.class);
	}

	@Test
	public void autoMigrateProcessesJob()
	{
		jobManager.executeJobAndWait(AutoMigrateProcessesJob.class);
	}
}
