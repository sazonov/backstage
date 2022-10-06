package com.proit.app.model.other.template;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class DocumentTemplateFilter
{
	@Builder.Default
	private final List<String> attachmentIds = new ArrayList<>();

	private final String name;
}
