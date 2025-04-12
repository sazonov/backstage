package com.proit.app.conversion;

import com.proit.app.conversion.dto.AbstractConverter;
import org.springframework.stereotype.Component;

@Component
public class NumberConverter extends AbstractConverter<Integer, String>
{
	@Override
	public String convert(Integer source)
	{
		return String.valueOf(source);
	}
}
