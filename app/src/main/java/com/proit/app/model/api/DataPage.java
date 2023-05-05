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

package com.proit.app.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DataPage
{
	private Integer pageNumber;

	private Integer pageSize;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Integer totalPages;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Long totalElements;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Boolean hasNext;

	public DataPage(Page<?> p)
	{
		this(p.getNumber(), p.getSize(), p.getTotalPages(), p.getTotalElements(), null);
	}

	public DataPage(Slice<?> p)
	{
		this(p.getNumber(), p.getSize(), null, null, p.hasNext());
	}
}
