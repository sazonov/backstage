package com.proit.bpm.jbpm.service.timer;

import com.proit.bpm.jbpm.AbstractTest;
import org.junit.jupiter.api.Test;

class QuartzTimerServiceTest extends AbstractTest
{
	@Test
	void migrateJobClassPackageTest() throws ClassNotFoundException
	{
		var oldClassName = "com.proit.bpm.service.jbpm.timer.QuartzTimerJob";
		Class.forName(oldClassName);
	}
}