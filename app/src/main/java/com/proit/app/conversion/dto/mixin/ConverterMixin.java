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

package com.proit.app.conversion.dto.mixin;

import com.proit.app.conversion.dto.Converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Используется для кастомизации работы конвертеров.
 * @param <C> Класс конвертера, который кастомизируем.
 * @param <T> Тип элемента, который генерирует конвертер.
 */
public interface ConverterMixin<C extends Converter<?, T>, T>
{
	void process(T convertedObject);

	default void process(List<T> convertedObjects)
	{
		convertedObjects.forEach(this::process);
	}

	default void process(T[] convertedObjects)
	{
		process(Arrays.asList(convertedObjects));
	}

	default void process(Stream<T> convertedObjects)
	{
		process(convertedObjects.collect(Collectors.toList()));
	}
}
