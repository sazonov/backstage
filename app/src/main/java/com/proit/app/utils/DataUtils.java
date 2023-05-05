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

package com.proit.app.utils;

import org.springframework.data.domain.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public class DataUtils
{
	private static final Pageable EMPTY_PAGEABLE = PageRequest.of(0, 20);
	private static final Page<?> EMPTY_PAGE = new PageImpl<>(Collections.emptyList(), EMPTY_PAGEABLE, 0);
	private static final Slice<?> EMPTY_SLICE = new SliceImpl<>(Collections.emptyList(), EMPTY_PAGEABLE, false);

	public static <T> Page<T> emptyPage()
	{
		return (Page<T>) EMPTY_PAGE;
	}

	public static <T> Page<T> emptyPage(Pageable pageable)
	{
		return new PageImpl<>(Collections.emptyList(), pageable, 0);
	}

	public static <T> Slice<T> emptySlice()
	{
		return (Slice<T>) EMPTY_SLICE;
	}

	public static <T> Slice<T> emptySlice(Pageable pageable)
	{
		return new SliceImpl<>(Collections.emptyList(), pageable, false);
	}

	public static <T> Page<T> paginateList(List<T> list, Pageable pageable)
	{
		int firstElementIndexOnPage = pageable.getPageNumber() * pageable.getPageSize();

		if (list.size() <= firstElementIndexOnPage)
		{
			return new PageImpl<>(Collections.emptyList(), pageable, list.size());
		}

		ArrayList<T> data = new ArrayList<>(pageable.getPageSize());
		ListIterator<T> iterator = list.listIterator(firstElementIndexOnPage);

		while (iterator.hasNext() && data.size() < pageable.getPageSize())
		{
			data.add(iterator.next());
		}

		return new PageImpl<>(data, pageable, list.size());
	}

	public static <T> void paginateList(List<T> list, int pageSize, Consumer<Page<T>> pageConsumer)
	{
		int pages = (int) Math.ceil((double) list.size() / (double) pageSize);

		for (int page = 0; page < pages; page++)
		{
			pageConsumer.accept(paginateList(list, PageRequest.of(page, pageSize)));
		}
	}
}
