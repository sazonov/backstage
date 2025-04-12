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

package com.proit.app.jobs.service;

import com.proit.app.jobs.model.dto.param.JobParams;

/**
 * Абстрактный класс для задач, которые запускаются только в ручном режиме.
 */
public abstract class AbstractManualJob<T extends JobParams> extends AbstractJob<T>
{
	public AbstractManualJob()
	{
		trigger = new NoOpTrigger();
	}
}
