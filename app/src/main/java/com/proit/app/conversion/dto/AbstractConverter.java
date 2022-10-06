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

package com.proit.app.conversion.dto;

import com.proit.app.utils.DataUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractConverter<S, T> implements  Converter<S, T>
{
	public List<T> convert(List<S> list)
	{
		return list.stream().map(this::convert).collect(Collectors.toList());
	}

	public List<T> convert(Collection<S> collection)
	{
		return convert(new ArrayList<>(collection));
	}

	public List<T> convert(S[] array)
	{
		return convert(Arrays.asList(array));
	}

	public Stream<T> convert(Stream<? extends S> stream)
	{
		return stream.map(this::convert);
	}

	public Page<T> convert(Page<S> page)
	{
		if (page.isEmpty())
		{
			return DataUtils.emptyPage();
		}

		return new PageImpl<>(convert(page.getContent()), page.getPageable(), page.getTotalElements());
	}

	public Slice<T> convert(Slice<S> slice)
	{
		if (slice.isEmpty())
		{
			return DataUtils.emptySlice();
		}

		var converted = convert(slice.getContent());

		return new SliceImpl<>(converted, slice.getPageable(), slice.hasNext());
	}
}
