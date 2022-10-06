package com.proit.app.service;

import com.proit.app.common.AbstractTest;
import com.proit.app.exception.QuerySyntaxError;
import com.proit.app.service.query.QueryParser;
import com.proit.app.service.query.ast.Constant;
import com.proit.app.service.query.ast.InExpression;
import com.proit.app.service.query.ast.LogicExpression;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class QueryParserTest extends AbstractTest
{
	@Autowired private QueryParser queryParser;

	@Test
	public void parse_timestampQueryTest()
	{
		var expression = queryParser.parse("timestampField in ('2020-03-01'::date, '2020-04-01T10:22:00+03:00'::date, '2020-04-01T10:22:00.555+03:00'::date)");

		assertTrue(expression instanceof InExpression);
		assertEquals(((InExpression) expression).variants.stream().filter(it -> it.type == Constant.Type.TIMESTAMP).count(), ((InExpression) expression).variants.size());
	}

	@Test
	public void parse_logicTest()
	{
		var expression = queryParser.parse("field1 = 1 and (field2 = 2 or field3 = 3) and not (field4 = 4)");

		assertTrue(expression instanceof LogicExpression);
		assertSame(((LogicExpression) expression).type, LogicExpression.Type.AND);
	}

	@Test
	public void parse_invalidQuery()
	{
		assertThrows(QuerySyntaxError.class, () -> queryParser.parse("stringField = stringField"));
	}
}