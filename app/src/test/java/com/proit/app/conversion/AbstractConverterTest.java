package com.proit.app.conversion;

import com.proit.app.common.AbstractTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
public class AbstractConverterTest extends AbstractTests
{
	private static final int PAGE_NUMBER = 1;
	private static final int PAGE_SIZE = 10;
	private static final int TOTAL_ELEMENTS = 7;

	@Autowired
	private NumberConverter numberConverter;

	@Test
	public void convertEmptyPage()
	{
		var page = numberConverter.convert(new PageImpl<>(Collections.emptyList(), PageRequest.of(PAGE_NUMBER, PAGE_SIZE), TOTAL_ELEMENTS));

		assertEquals(page.getPageable().getPageNumber(), PAGE_NUMBER);
		assertEquals(page.getPageable().getPageSize(), PAGE_SIZE);
		assertEquals(page.getTotalElements(), TOTAL_ELEMENTS);
	}

	@Test
	public void convertEmptySlice()
	{
		var slice = numberConverter.convert(new SliceImpl<>(Collections.emptyList(), PageRequest.of(PAGE_NUMBER, PAGE_SIZE), false));

		assertEquals(slice.getPageable().getPageNumber(), PAGE_NUMBER);
		assertEquals(slice.getPageable().getPageSize(), PAGE_SIZE);
		assertFalse(slice.hasNext());
	}
}
