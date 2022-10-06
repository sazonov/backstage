package com.proit.app.model.dto.template.request;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SearchDocumentTemplateRequest
{
	private String name;

	private List<String> attachmentIds = new ArrayList<>();
}
