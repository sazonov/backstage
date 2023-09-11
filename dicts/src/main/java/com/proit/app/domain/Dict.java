/*
 *
 *  Copyright 2019-2023 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.proit.app.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dict
{
	private String id;

	private String name;

	@Builder.Default
	private List<DictField> fields = new ArrayList<>();

	@Builder.Default
	private List<DictIndex> indexes = new ArrayList<>();

	@Builder.Default
	private List<DictConstraint> constraints = new ArrayList<>();

	@Builder.Default
	private List<DictEnum> enums = new ArrayList<>();

	private String viewPermission;

	private String editPermission;

	private LocalDateTime deleted;

	private DictEngine engine;
}
