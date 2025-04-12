/*
 *    Copyright 2019-2024 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.proit.app.dict.service.query;

import com.proit.app.dict.exception.dict.query.QuerySyntaxException;
import com.proit.app.dict.service.query.ast.*;
import org.apache.commons.lang3.StringUtils;
import org.jparsec.*;

import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

public class QueryParser
{
	final String[] OPERATORS = { "=", "!=", "<>", "<", ">", "<=", ">=", ",", "(", ")", "::", ".", "[", "]" };

	final String[] KEYWORDS = { "in", "like", "ilike", "any", "all", "true", "false", "not", "and", "or", "null" };

	final Terminals TERMS = Terminals.operators(OPERATORS).words(Scanners.IDENTIFIER).caseInsensitiveKeywords(KEYWORDS).build();

	final Parser<?> TOKENIZER = Parsers.or(Terminals.DecimalLiteral.TOKENIZER, Terminals.IntegerLiteral.TOKENIZER, Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER, Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER, TERMS.tokenizer());

	Parser<?> term(String term)
	{
		return TERMS.token(term);
	}

	/**
	 *
	 * Синтаксические правила.
	 *
	 */

	final Parser<Constant> DECIMAL_CONSTANT = Terminals.DecimalLiteral.PARSER.map(str -> new Constant(str, Constant.Type.DECIMAL));

	final Parser<Constant> INT_CONSTANT = Terminals.IntegerLiteral.PARSER.map(str -> new Constant(str, Constant.Type.INTEGER));

	final Parser<Constant> STRING_CONSTANT = Terminals.StringLiteral.PARSER.map(str -> new Constant(str, Constant.Type.STRING));

	final Parser<Constant> BOOLEAN_CONSTANT = Parsers.or(
		term("true").retn(new Constant("true", Constant.Type.BOOLEAN)),
		term("false").retn(new Constant("false", Constant.Type.BOOLEAN))
	);

	final Parser<Constant> NULL_CONSTANT = term("null").retn(new Constant(null, Constant.Type.NULL));

	final Parser<Constant> SIMPLE_CONSTANT = Parsers.or(DECIMAL_CONSTANT, INT_CONSTANT, STRING_CONSTANT, BOOLEAN_CONSTANT, NULL_CONSTANT);
	final Parser<Constant> CASTED_CONSTANT = Parsers.sequence(SIMPLE_CONSTANT, term("::"), Terminals.identifier(), (constant, kw, targetType) -> new Constant(constant, targetType));
	final Parser<Constant> CONSTANT = Parsers.or(CASTED_CONSTANT, SIMPLE_CONSTANT);

	final Parser<Field> FIELD = Parsers.or(Parsers.sequence(Terminals.identifier(), term("."), Terminals.identifier(), (dict, kw, field) -> new Field(dict, field)),
			Terminals.Identifier.PARSER.map(Field::new));

	final Parser<List<Constant>> CONSTANT_LIST = Parsers.between(term("("), CONSTANT.sepBy(term(",")), term(")"));

	final Parser<List<Constant>> CONSTANT_ARRAY = Parsers.between(term("["), CONSTANT.sepBy(term(",")), term("]"));

	final Parser<Predicate.Type> CompOp = Parsers.or(
		term("=").retn(Predicate.Type.EQ),
		term("<").retn(Predicate.Type.LS),
		term(">").retn(Predicate.Type.GT),
		term("<=").retn(Predicate.Type.LEQ),
		term(">=").retn(Predicate.Type.GEQ),
		term("!=").retn(Predicate.Type.NEQ),
		term("<>").retn(Predicate.Type.NEQ)
	);

	final Parser<AllOrAnyQueryExpression.Type> ArrayOp = Parsers.or(
			term("all").retn(AllOrAnyQueryExpression.Type.ALL),
			term("any").retn(AllOrAnyQueryExpression.Type.ANY)
	);

	final Parser<InQueryExpression> IN_EXPR = Parsers.sequence(FIELD, term("in"), CONSTANT_LIST, (field, kw, constants) -> new InQueryExpression(field, constants));

	final Parser<LikeQueryExpression> LIKE_EXPR = Parsers.sequence(FIELD, term("like"), STRING_CONSTANT, (field, kw, regExp) -> new LikeQueryExpression(field, regExp));

	final Parser<IlikeQueryExpression> ILIKE_EXPR = Parsers.sequence(FIELD, term("ilike"), STRING_CONSTANT, (field, kw, regExp) -> new IlikeQueryExpression(field, regExp));

	final Parser<AllOrAnyQueryExpression> ALL_OR_ANY_EXPR = Parsers.sequence(FIELD, ArrayOp, CONSTANT_ARRAY, AllOrAnyQueryExpression::new);

	final Parser<Predicate> COMPARE_EXPR = Parsers.sequence(FIELD, CompOp, CONSTANT, (lhs, type, rhs) -> new Predicate(lhs, rhs, type));

	final Parser<QueryExpression> PREDICATE = Parsers.or(COMPARE_EXPR, LIKE_EXPR, IN_EXPR, ILIKE_EXPR, ALL_OR_ANY_EXPR);

	final UnaryOperator<QueryExpression> NOT = expr -> new LogicQueryExpression(expr, null, LogicQueryExpression.Type.NOT);
	final BinaryOperator<QueryExpression> AND = (lhs, rhs) -> new LogicQueryExpression(lhs, rhs, LogicQueryExpression.Type.AND);
	final BinaryOperator<QueryExpression> OR = (lhs, rhs) -> new LogicQueryExpression(lhs, rhs, LogicQueryExpression.Type.OR);

	final Parser.Reference<QueryExpression> QUERY_REF = Parser.newReference();

	Parser<QueryExpression> LogicalExpr(Parser<QueryExpression> predicate)
	{
		Parser.Reference<QueryExpression> ref = Parser.newReference();

		Parser<QueryExpression> parser = new OperatorTable<QueryExpression>()
				.prefix(term("not").retn(NOT), 30)
				.infixl(term("and").retn(AND), 20)
				.infixl(term("or").retn(OR),   10)
				.build(Parsers.between(term("("), ref.lazy(), term(")")).or(predicate));

		ref.set(parser);

		return parser;
	}

	final Parser<QueryExpression> QUERY = LogicalExpr(PREDICATE);

	{
		QUERY_REF.set(QUERY);
	}

	final Parser<QueryExpression> GRAMMAR = QUERY.or(Parsers.EOF.retn(new Empty()));

	public QueryExpression parse(String input) throws QuerySyntaxException
	{
		try
		{
			return GRAMMAR.from(TOKENIZER, Scanners.SQL_DELIMITER).parse(input != null ? input : StringUtils.EMPTY);
		}
		catch (Exception ex)
		{
			throw new QuerySyntaxException(ex);
		}
	}
}
