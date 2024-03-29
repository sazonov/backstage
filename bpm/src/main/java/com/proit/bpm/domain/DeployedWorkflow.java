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

package com.proit.bpm.domain;

import com.proit.app.database.model.NonCacheableEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "workflow")
public class DeployedWorkflow implements NonCacheableEntity
{
	@Id
	private String id;

	@Temporal(TemporalType.TIMESTAMP)
	private LocalDateTime created;

	@Temporal(TemporalType.TIMESTAMP)
	private LocalDateTime archived;

	@Lob
	private String definition;

	private String checksum;
}
