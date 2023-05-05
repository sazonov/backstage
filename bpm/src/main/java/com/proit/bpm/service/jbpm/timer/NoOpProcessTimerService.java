/*
 *    Copyright 2019-2023 the original author or authors.
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

import com.proit.bpm.model.ProcessTimer;
import com.proit.bpm.service.ProcessTimerService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class NoOpProcessTimerService implements ProcessTimerService
{
	public List<ProcessTimer> getProcessTimers(String processId)
	{
		return Collections.emptyList();
	}

	public void updateProcessTimer(String processId, String timerName, LocalDateTime nextFireTime)
	{
	}
}
