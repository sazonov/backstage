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

package com.proit.app.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.proit.app.model.other.exception.ApiStatusCategory;
import com.proit.app.model.other.exception.ApiStatusCode;
import com.proit.app.model.other.exception.ApiStatusCodeImpl;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.stream.Stream;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiResponse<T>
{
	private String message;

	private Integer status;

	@JsonIgnore
	private ApiStatusCategory category;

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private String[] stackTrace;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String exceptionCode;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private T data;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private DataPage paging;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private List<ApiValidationError> validationErrors;

	public static OkResponse ok()
	{
		return OkResponse.INSTANCE;
	}

	public static ApiResponse<Object> of(ApiStatusCode statusCode)
	{
		return new ApiResponse<>(statusCode);
	}

	public static ApiResponse<Object> of(ApiStatusCode statusCode, String message)
	{
		return new ApiResponse<>(statusCode, message);
	}

	public static <T> ApiResponse<T> of(T data)
	{
		return new ApiResponse<>(data);
	}

	public static <T> ApiResponse<T> of(T data, Page<T> page)
	{
		return new ApiResponse<>(data, page);
	}

	public static <T> ApiResponse<List<T>> of(List<T> data, Page<T> page)
	{
		return new ApiResponse<>(data, page);
	}

	public static <T> ApiResponse<List<T>> of(Stream<T> data)
	{
		return ApiResponse.of(data.toList());
	}

	public static <T> ApiResponse<List<T>> of(Page<T> page)
	{
		return ApiResponse.of(page.getContent(), page);
	}

	public static <T> ApiResponse<List<T>> of(Slice<T> slice)
	{
		return new ApiResponse<>(slice.getContent(), slice);
	}

	ApiResponse(ApiStatusCode statusCode)
	{
		this(statusCode, statusCode.getMessage());
	}

	private ApiResponse(ApiStatusCode statusCode, String message)
	{
		this.status = statusCode.getCode();
		this.category = statusCode.getCategory();
		this.message = message;
	}

	private ApiResponse(T data)
	{
		this(ApiStatusCodeImpl.OK);

		this.data = data;
	}

	private ApiResponse(T data, Page<?> page)
	{
		this(ApiStatusCodeImpl.OK);

		this.data = data;
		this.paging = new DataPage(page);
	}

	private ApiResponse(T data, Slice<?> slice)
	{
		this(ApiStatusCodeImpl.OK);

		this.data = data;
		this.paging = new DataPage(slice);
	}
}
