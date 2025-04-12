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

package com.proit.app.database.utils;

import com.proit.app.database.model.Identity;
import lombok.experimental.UtilityClass;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

@UtilityClass
public class StreamUtils
{
	/**
	 * Служит для упорядочивания элементов по вхождению их идентификаторов в упорядоченную коллекцию.
	 * Типовой сценарий - запросили из репозитория список сущностей по идентификаторам, отсортировали в порядке,
	 * в котором мы их запрашивали.
	 * @param ids - упорядоченный список идентификаторов сущностей, которых планируем отсортировать
	 * @param <I> - тип идентификатора сущности
	 * @param <T> - тип сущности
	 */
	public static <I, T extends Identity<I>> Comparator<T> listOrderComparator(List<I> ids)
	{
		return Comparator.comparingInt(it -> ids.indexOf(it.getId()));
	}

	/**
	 * Обработка и закрытие стрима.
	 * @param stream - стрим
	 * @param action - действие
	 * @param <T> - тип элементов
	 */
	public static <T> void processAndClose(Stream<T> stream, Consumer<T> action)
	{
		try (stream)
		{
			stream.forEach(action);
		}
	}
}
