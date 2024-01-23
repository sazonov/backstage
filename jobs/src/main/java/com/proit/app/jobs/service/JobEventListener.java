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

package com.proit.app.jobs.service;

import com.proit.app.jobs.model.dto.other.JobResult;

public interface JobEventListener
{
	void beforeSchedule(AbstractJob<?> job);

	void afterSchedule(AbstractJob<?> job);

	void beforeExecute(AbstractJob<?> job);

	void afterExecute(AbstractJob<?> job, JobResult jobResult);

	void onCancel(AbstractJob<?> job);

	void onException(AbstractJob<?> job, Exception exception);
}
