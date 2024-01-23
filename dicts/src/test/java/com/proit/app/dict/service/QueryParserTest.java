package com.proit.app.dict.service;

import com.proit.app.dict.common.CommonTest;
import com.proit.app.dict.exception.dict.query.QuerySyntaxException;
import com.proit.app.dict.service.query.QueryParser;
import com.proit.app.dict.service.query.ast.Constant;
import com.proit.app.dict.service.query.ast.InQueryExpression;
import com.proit.app.dict.service.query.ast.LogicQueryExpression;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class QueryParserTest extends CommonTest
{
	@Autowired
	private QueryParser queryParser;

	@Test
	public void parse_timestampQueryTest()
	{
		var expression = queryParser.parse("timestampField in ('2020-03-01'::timestamp, '2020-03-01+03:00'::timestamp, '2020-04-01T10:22:00'::timestamp, '2020-04-01T10:22:00.555'::timestamp, '2020-04-01T10:22:00+03:00'::timestamp, '2020-04-01T10:22:00.555+03:00'::timestamp)");

		assertTrue(expression instanceof InQueryExpression);

		var expected = ((InQueryExpression) expression).variants.stream().filter(it -> it.type == Constant.Type.TIMESTAMP).count();
		var actual = ((InQueryExpression) expression).variants.size();

		assertEquals(expected, actual);
	}

	@Test
	public void parse_dateQueryTest()
	{
		var expression = queryParser.parse("timestampField in ('2020-03-01'::date, '2020-03-01+03:00'::date, '2020-04-01T10:22:00'::date, '2020-04-01T10:22:00.555'::date, '2020-04-01T10:22:00+03:00'::date, '2020-04-01T10:22:00.555+03:00'::date)");

		assertTrue(expression instanceof InQueryExpression);

		var expected = ((InQueryExpression) expression).variants.stream().filter(it -> it.type == Constant.Type.DATE).count();
		var actual = ((InQueryExpression) expression).variants.size();

		assertEquals(expected, actual);
	}

	@Test
	public void parse_logicTest()
	{
		var expression = queryParser.parse("field1 = 1 and (field2 = 2 or field3 = 3) and not (field4 = 4)");

		assertTrue(expression instanceof LogicQueryExpression);

		var expected = ((LogicQueryExpression) expression).type;
		var actual = LogicQueryExpression.Type.AND;

		assertSame(expected, actual);
	}

	@Test
	public void parse_invalidQuery()
	{
		assertThrows(QuerySyntaxException.class, () -> queryParser.parse("stringField = stringField"));
	}
}