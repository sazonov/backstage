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

package com.proit.app.api.utils;

import com.proit.app.api.model.ApiResponse;
import com.proit.app.exception.AppException;
import com.proit.app.model.other.exception.ApiStatusCodeImpl;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
@UtilityClass
public class RemoteServiceUtils
{
	public static <T> ApiResponse<T> execute(Supplier<ApiResponse<T>> service)
	{
		try
		{
			return service.get();
		}
		catch (Exception e)
		{
			throw new AppException(ApiStatusCodeImpl.REMOTE_SERVICE_ERROR, e);
		}
	}

	public static <T> T executeAndGetData(Supplier<ApiResponse<T>> service)
	{
		var response = service.get();

		if (response.getStatus() > 0)
		{
			throw new AppException(ApiStatusCodeImpl.REMOTE_SERVICE_ERROR, response.getMessage());
		}
		else
		{
			return response.getData();
		}
	}

	public static <T> List<T> fetchList(Supplier<ApiResponse<List<T>>> service)
	{
		return fetchList(service, null);
	}

	public static <T> List<T> fetchList(Supplier<ApiResponse<List<T>>> service, List<T> defaultList)
	{
		var response = execute(service);

		if (response.getStatus() > 0)
		{
			if (defaultList != null)
			{
				log.warn("{} {}", ApiStatusCodeImpl.REMOTE_SERVICE_ERROR.getMessage(), response.getMessage());

				return defaultList;
			}
			else
			{
				throw new AppException(ApiStatusCodeImpl.REMOTE_SERVICE_ERROR, response.getMessage());
			}
		}

		if (response.getData() == null)
		{
			if (defaultList != null)
			{
				log.warn("{} {}", ApiStatusCodeImpl.REMOTE_SERVICE_ERROR.getMessage(), "Ответ сервиса не содержит список.");

				return defaultList;
			}
			else
			{
				throw new AppException(ApiStatusCodeImpl.REMOTE_SERVICE_ERROR, "Ответ сервиса не содержит список.");
			}
		}

		return response.getData();
	}

	public static <T> Page<T> fetchPage(Supplier<ApiResponse<List<T>>> service)
	{
		return fetchPage(service, null);
	}

	public static <T> Page<T> fetchPage(Supplier<ApiResponse<List<T>>> service, Page<T> defaultPage)
	{
		var response = execute(service);

		if (response.getStatus() > 0)
		{
			if (defaultPage != null)
			{
				log.warn("{} {}", ApiStatusCodeImpl.REMOTE_SERVICE_ERROR.getMessage(), response.getMessage());

				return defaultPage;
			}
			else
			{
				throw new AppException(ApiStatusCodeImpl.REMOTE_SERVICE_ERROR, response.getMessage());
			}
		}

		if (response.getPaging() == null || response.getData() == null)
		{
			if (defaultPage != null)
			{
				log.warn("{} {}", ApiStatusCodeImpl.REMOTE_SERVICE_ERROR.getMessage(), "Ответ сервиса не содержит страницу.");

				return defaultPage;
			}
			else
			{
				throw new AppException(ApiStatusCodeImpl.REMOTE_SERVICE_ERROR, "Ответ сервиса не содержит страницу.");
			}
		}

		var paging = response.getPaging();

		return new PageImpl<>(response.getData(), PageRequest.of(paging.getPageNumber(), paging.getPageSize()), paging.getTotalElements());
	}
}
