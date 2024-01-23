package com.proit.app.api.controller.request;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class CoffeeRequest
{
	private CupSize size;

	private List<CoffeeType> types;

	private Map<Additive, Integer> additives;

	private Wish wish;

	private List<Wish> wishes;

	public enum CoffeeType
	{
		LATTE, CAPPUCCINO, AMERICANO, ESPRESSO, FLAT_WHITE
	}

	public enum CupSize
	{
		SMALL, MEDIUM, BIG
	}

	public enum Additive
	{
		SUGAR, MILK, SYRUP
	}

	@Getter
	public static class Wish
	{
		public MilkTemperature temperature;

		public List<MilkTemperature> temperatures;

		public Map<MilkTemperature, Integer> temperatureCounts;

		public enum MilkTemperature
		{
			HOT, COLD, WARM
		}
	}
}
