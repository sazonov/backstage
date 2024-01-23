package com.proit.app.jobs.service;

import lombok.RequiredArgsConstructor;
import com.proit.app.jobs.model.dto.param.JobParams;

@RequiredArgsConstructor
public class JobExecutionContext<J extends AbstractJob<? super P>, P extends JobParams> implements Runnable
{
	private final J job;
	private final P params;

	@Override
	public void run()
	{
		job.execute(params);
	}
}
