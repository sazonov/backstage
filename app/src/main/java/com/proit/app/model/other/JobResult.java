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

package com.proit.app.model.other;

import lombok.*;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class JobResult
{
	@Builder.Default
	private final boolean success = true;

	@Builder.Default
	private final Map<String, Object> properties = new HashMap<>();

	public static JobResult ok()
	{
		return ok(Map.of());
	}

	public static JobResult ok(Map<String, Object> properties)
	{
		return JobResult.builder()
				.properties(properties)
				.build();
	}

	public static JobResult failed()
	{
		return failed(Map.of());
	}

	public static JobResult failed(Throwable throwable)
	{
		return failed(Map.of(
				"message", throwable.getMessage(),
				"rootCauseMessage", ExceptionUtils.getRootCauseMessage(throwable),
				"stackTrace", ExceptionUtils.getStackFrames(throwable)));
	}

	public static JobResult failed(Map<String, Object> properties)
	{
		return JobResult.builder()
				.success(false)
				.properties(properties)
				.build();
	}
}
