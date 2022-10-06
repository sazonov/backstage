/*
 *    Copyright 2019-2022 the original author or authors.
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

package com.proit.bpm.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.util.Version;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class Workflow
{
	private String id;

	private String name;

	private Version version;

	private LocalDateTime created;

	private String definition;

	Map<String, WorkflowScript> scripts = new HashMap<>();

	public String getFullId()
	{
		return id + "_" + version;
	}
}
