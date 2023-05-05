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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ExceptionUtils
{
	public static <T extends RuntimeException> void throwExtracted(Throwable target, Class<T> clazz, RuntimeException defaultValue)
	{
		T extracted = extract(target, clazz);

		if (extracted != null)
		{
			throw extracted;
		}
		else
		{
			throw defaultValue;
		}
	}

	public static <T extends Throwable> T extract(Throwable target, Class<T> clazz)
	{
		List<Throwable> history = new LinkedList<>();
		List<T> rightThrows = new ArrayList<>();

		if (clazz.isInstance(target))
		{
			rightThrows.add(clazz.cast(target));
		}

		return innerExtract(target, clazz, history, rightThrows);
	}

	private static <T> T innerExtract(Throwable target, Class<T> clazz, List<Throwable> history, List<T> rightThrows)
	{
		Throwable cause = target.getCause();

		if (cause != null)
		{
			if (clazz.isInstance(cause))
			{
				rightThrows.add(clazz.cast(cause));

				return innerExtract(cause, clazz, history, rightThrows);
			}
			else
			{
				if (!history.contains(cause))
				{
					history.add(cause);

					return innerExtract(cause, clazz, history, rightThrows);
				}
			}
		}
		else
		{
			if (!rightThrows.isEmpty())
			{
				return rightThrows.get(rightThrows.size() - 1);
			}
		}

		return null;
	}
}