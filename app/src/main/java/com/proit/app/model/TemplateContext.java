package com.proit.app.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class TemplateContext
{
	private String filename;

	private Map<String, Object> data = new HashMap<>();
}
