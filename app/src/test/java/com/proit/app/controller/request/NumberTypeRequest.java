package com.proit.app.controller.request;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class NumberTypeRequest
{
	private Integer fieldInteger;

	private Double fieldDouble;

	private List<Integer> integers;

	private Map<Integer, Double> map;
}
