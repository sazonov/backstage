package com.proit.app.service.job;

import com.proit.app.model.dto.job.JobParams;
import lombok.RequiredArgsConstructor;

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
