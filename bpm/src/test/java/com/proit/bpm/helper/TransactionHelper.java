package com.proit.bpm.helper;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionHelper
{
	@Transactional
	public void runInTransaction(Runnable runnable)
	{
		runnable.run();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void runInNewTransaction(Runnable runnable)
	{
		runnable.run();
	}
}
