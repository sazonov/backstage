package com.proit.app.configuration.springdoc.models;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springdoc.api.annotations.ParameterObject;

import javax.validation.constraints.Min;
import java.util.List;

@ParameterObject
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class Pageable
{
	/**
	 * The Page.
	 */
	@Min(0)
	@Parameter(description = "Zero-based page index (0..N)",
			schema = @Schema(type = "integer", defaultValue = "0"))
	private Integer page;

	/**
	 * The Size.
	 */
	@Min(1)
	@Parameter(description = "The size of the page to be returned",
			schema = @Schema(type = "integer", defaultValue = "20"))
	private Integer size;

	/**
	 * The Sort.
	 */
	@Parameter(description = """
			Sorting criteria in the format: property,(asc|desc).
			Default sort order is ascending.
			Multiple sort criteria are supported.
			""",
			name = "sort",
			array = @ArraySchema(schema = @Schema(type = "string")))
	private List<String> sort;
}