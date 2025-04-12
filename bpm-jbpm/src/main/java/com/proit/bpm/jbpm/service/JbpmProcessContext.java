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

package com.proit.bpm.jbpm.service;

import com.proit.bpm.domain.Process;
import com.proit.bpm.model.ProcessContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.kie.api.runtime.KieSession;

@Getter
@RequiredArgsConstructor
public class JbpmProcessContext implements ProcessContext
{
	private final Process process;

	private final KieSession session;

	public Long getProcessInstanceId()
	{
		return Long.parseLong(process.getInstanceId());
	}
}
