/*
 *    Copyright 2019-2024 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.proit.bpm.service.jbpm.timer;

import com.proit.bpm.jbpm.service.timer.QuartzTimerService;
import com.proit.bpm.repository.ProcessRepository;
import com.proit.bpm.service.process.ProcessEngine;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @deprecated Нужен исключительно для совместимости со старыми версиями JobDetail.
 *              Использовать {@link com.proit.bpm.jbpm.service.timer.QuartzTimerJob}.
 */
@PersistJobDataAfterExecution
@Deprecated(since = "4.7", forRemoval = true)
final class QuartzTimerJob extends com.proit.bpm.jbpm.service.timer.QuartzTimerJob
{
	public QuartzTimerJob(ProcessRepository processRepository,
	                      @Qualifier("jbpmProcessEngine") ProcessEngine processEngine,
	                      QuartzTimerService quartzTimerService)
	{
		super(processRepository, processEngine, quartzTimerService);
	}
}
