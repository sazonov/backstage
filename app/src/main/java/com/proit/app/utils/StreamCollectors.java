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

package com.proit.app.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;

/**
 * Класс содержит реализации кастомных коллекторов.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StreamCollectors
{
	/**
	 * Коллектор который не выбрасывает {@link NullPointerException} при value == null.
	 * <p>
	 * Стандартный коллектор {@link java.util.stream.Collectors#toMap(Function keyMapper, Function valueMapper)}
	 * содержит проверку Objects.requireNonNull() для value.
	 *
	 * @see java.util.stream.Collectors
	 * @return LinkedHashMap
	 */
	public static <T, K, U>
	Collector<T, ?, Map<K, U>> toLinkedHashMap(Function<? super T, ? extends K> keyMapper,
	                                           Function<? super T, ? extends U> valueMapper)
	{
		return Collector.of(LinkedHashMap::new,
				(o, n) -> o.put(keyMapper.apply(n), valueMapper.apply(n)),
				(n, o) -> {
					n.putAll(o);
					return n;
				});
	}
}
