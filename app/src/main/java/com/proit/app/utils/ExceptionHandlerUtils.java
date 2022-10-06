package com.proit.app.utils;

import com.proit.app.model.api.ApiValidationError;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;

import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ExceptionHandlerUtils
{
	public static List<ApiValidationError> buildValidationErrors(Errors ex)
	{
		var validationErrors = new ArrayList<ApiValidationError>();

		ex.getFieldErrors()
				.stream()
				.map(fieldError -> {
					var apiError = new ApiValidationError();
					apiError.setResource(fieldError.getObjectName());
					apiError.setField(fieldError.getField());

					if (fieldError.getDefaultMessage() != null && fieldError.getDefaultMessage().startsWith("validation."))
					{
						apiError.setCode(fieldError.getDefaultMessage());
					}
					else
					{
						apiError.setCode(fieldError.getCode());
						apiError.setDescription(fieldError.getDefaultMessage());
					}

					return apiError;
				})
				.forEach(validationErrors::add);

		ex.getGlobalErrors()
				.stream()
				.map(globalError ->
						ApiValidationError.builder()
								.resource(globalError.getObjectName())
								.code(globalError.getCode())
								.build())
				.forEach(validationErrors::add);

		validationErrors.forEach(validationError -> {
			if (validationError.getCode() != null && validationError.getCode().startsWith("validation."))
			{
				validationError.setCode(validationError.getCode());
				validationError.setDescription(ValidationUtils.errorCodeToMessage(validationError.getCode()));
			}
		});

		return validationErrors;
	}

	public static List<ApiValidationError> buildValidationErrors(ConstraintViolationException ex)
	{
		return ex.getConstraintViolations()
				.stream()
				.map(violation ->
						ApiValidationError.builder()
								.resource(StringUtils.substringBefore(violation.getPropertyPath().toString(), "."))
								.field(StringUtils.substringAfter(violation.getPropertyPath().toString(), "."))
								.description(violation.getMessage())
								.build())
				.toList();
	}
}
