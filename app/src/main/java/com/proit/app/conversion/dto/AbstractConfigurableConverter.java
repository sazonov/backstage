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

public abstract class AbstractConfigurableConverter<S, T, C> implements  ConfigurableConverter<S, T, C>
{
	public T convert(S source)
	{
		return convert(source, configure());
	}

	public List<T> convert(List<S> list, C configuration)
	{
		return list.stream().map(it -> convert(it, configuration)).collect(Collectors.toList());
	}

	public List<T> convert(List<S> list)
	{
		return convert(list, configure());
	}

	public List<T> convert(S[] array, C configuration)
	{
		return convert(Arrays.asList(array), configuration);
	}

	public List<T> convert(S[] array)
	{
		return convert(Arrays.asList(array), configure());
	}

	public List<T> convert(Collection<S> collection)
	{
		return convert(new ArrayList<>(collection), configure());
	}

	public List<T> convert(Collection<S> collection, C configuration)
	{
		return convert(new ArrayList<>(collection), configuration);
	}

	public Stream<T> convert(Stream<? extends S> stream, C configuration)
	{
		return stream.map(it -> convert(it, configuration));
	}

	public Stream<T> convert(Stream<? extends S> stream)
	{
		return convert(stream, configure());
	}

	public Page<T> convert(Page<S> page, C configuration)
	{
		if (page.isEmpty())
		{
			return DataUtils.emptyPage();
		}

		List<T> converted = convert(page.getContent(), configuration);

		return new PageImpl<>(converted, page.getPageable(), page.getTotalElements());
	}

	public Page<T> convert(Page<S> page)
	{
		return convert(page, configure());
	}

	public Slice<T> convert(Slice<S> slice, C configuration)
	{
		if (slice.isEmpty())
		{
			return DataUtils.emptySlice();
		}

		var converted = convert(slice.getContent(), configuration);

		return new SliceImpl<>(converted, slice.getPageable(), slice.hasNext());
	}

	public Slice<T> convert(Slice<S> slice)
	{
		return convert(slice, configure());
	}
}
