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

package com.proit.app.jobs.model.dto.param;

/**
 * Example of implementation:
 * <pre>{@code
 *      @Schema(description = "Some params description using Swagger")
 *      public class JobParamsImplementation implements JobParams
 *      {
 *          @Schema(description = "Nullable-поле")
 *          String nullableField;
 *
 *          @NotEmpty
 *          @Schema(description = "Не пустое строковое поле с использованием javax.validation")
 *          String notEmptyField;
 *
 *          @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DateConstants.API_TIMESTAMP_FORMAT)
 *          @Schema(description = "Поле даты с паттерном, указанным через аннотацию из com.fasterxml.jackson")
 *          private ZonedDateTime dateField;
 * }
 *  }</pre>
 */
public interface JobParams
{
}
