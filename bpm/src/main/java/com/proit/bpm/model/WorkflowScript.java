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

package com.proit.bpm.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public class WorkflowScript
{
	@RequiredArgsConstructor
	public enum Type
	{
		GROOVY("groovy"),
		KOTLIN("kt");

		@Getter
		private final String ext;

		public static Type fromExtension(String filename)
		{
			return Arrays.stream(Type.values()).filter(
					it -> filename.toLowerCase().endsWith(it.getExt())).findFirst().orElseThrow(
							() -> new RuntimeException("unknown script type: %s".formatted(filename)));
		}

		public static List<String> allExtensions()
		{
			return Arrays.stream(values()).map(Type::getExt).toList();
		}
	}

	private String id;

	private Type type;

	private String definition;

	private Workflow workflow;

	public WorkflowScript(Workflow workflow)
	{
		this.workflow = workflow;
	}

	public String getFilename()
	{
		return getId() + "." + getType().getExt();
	}
}
