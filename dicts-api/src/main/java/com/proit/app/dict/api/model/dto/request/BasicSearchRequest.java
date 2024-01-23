package com.proit.app.dict.api.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Базовый поисковый запрос для записей справочника")
public class BasicSearchRequest
{
	@Schema(description = "Строка поиска (QL)")
	private String query;
}
