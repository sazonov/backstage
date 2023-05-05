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

package com.proit.bpm.conversion.dto;

import com.proit.app.conversion.dto.AbstractConverter;
import com.proit.bpm.model.Workflow;
import com.proit.bpm.model.dto.WorkflowDto;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
public class WorkflowConverter extends AbstractConverter<Workflow, WorkflowDto>
{
	public WorkflowDto convert(Workflow source)
	{
		var target = new WorkflowDto();

		target.setId(source.getId());
		target.setName(source.getName());
		target.setCreated(source.getCreated().atZone(ZoneId.systemDefault()));
		target.setVersion(source.getVersion().toString());

		return target;
	}
}
