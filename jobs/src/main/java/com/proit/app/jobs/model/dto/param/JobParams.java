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
