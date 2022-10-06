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

package com.proit.app.utils;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class TaskUtils
{
	@FunctionalInterface
	public interface VoidTask
	{
		void execute();
	}

	public static void executeWithTryCount(int tryCount, long delay, VoidTask task) throws Exception
	{
		executeWithTryCount(tryCount, delay, () -> { task.execute(); return true; }, (c, e) -> true);
	}

	public static <T> T executeWithTryCount(int tryCount, long delay, Supplier<T> task) throws Exception
	{
		return executeWithTryCount(tryCount, delay, task, (c, e) -> true);
	}

	public static <T> T executeWithTryCount(int tryCount, long delay, Supplier<T> task, BiFunction<Integer, Throwable, Boolean> errorHandler) throws Exception
	{
		int c = 0;

		Exception lastException = null;

		while (c < tryCount)
		{
			try
			{
				return task.get();
			}
			catch (Exception e)
			{
				lastException = e;

				if (!errorHandler.apply(c, e))
				{
					break;
				}

				c++;

				if (c < tryCount)
				{
					Thread.sleep(delay);
				}
			}
		}

		throw new RuntimeException("failed to execute task", lastException);
	}
}
