/*
 *    Copyright 2019-2022 the original author or authors.
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

package com.proit.app.service.query;

import com.proit.app.exception.QuerySyntaxError;
import com.proit.app.service.query.ast.*;
import org.jparsec.*;

import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

public class QueryParser
{
	final String[] OPERATORS = { "=", "!=", "<>", "<", ">", "<=", ">=", ",", "(", ")", "::" };

	final String[] KEYWORDS = { "in", "like", "ilike", "true", "false", "not", "and", "or", "null" };

	final Terminals TERMS = Terminals.operators(OPERATORS).words(Scanners.IDENTIFIER).caseInsensitiveKeywords(KEYWORDS).build();

	final Parser<?> TOKENIZER = Parsers.or(Terminals.IntegerLiteral.TOKENIZER, Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER, Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER, TERMS.tokenizer());

	Parser<?> term(String term)
	{
		return TERMS.token(term);
	}

	/**
	 *
	 * Синтаксические правила.
	 *
	 */

	final Parser<Constant> INT_CONSTANT = Terminals.IntegerLiteral.PARSER.map(str -> new Constant(str, Constant.Type.INTEGER));

	final Parser<Constant> STRING_CONSTANT = Terminals.StringLiteral.PARSER.map(str -> new Constant(str, Constant.Type.STRING));

	final Parser<Constant> BOOLEAN_CONSTANT = Parsers.or(
		term("true").retn(new Constant("true", Constant.Type.BOOLEAN)),
		term("false").retn(new Constant("false", Constant.Type.BOOLEAN))
	);

	final Parser<Constant> NULL_CONSTANT = term("null").retn(new Constant(null, Constant.Type.NULL));

	final Parser<Constant> SIMPLE_CONSTANT = Parsers.or(INT_CONSTANT, STRING_CONSTANT, BOOLEAN_CONSTANT, NULL_CONSTANT);
	final Parser<Constant> CASTED_CONSTANT = Parsers.sequence(SIMPLE_CONSTANT, term("::"), Terminals.identifier(), (constant, kw, targetType) -> new Constant(constant, targetType));
	final Parser<Constant> CONSTANT = Parsers.or(CASTED_CONSTANT, SIMPLE_CONSTANT);

	final Parser<Field> FIELD = Terminals.Identifier.PARSER.map(Field::new);

	final Parser<List<Constant>> CONSTANT_LIST = Parsers.between(term("("), CONSTANT.sepBy(term(",")), term(")"));

	final Parser<Predicate.Type> CompOp = Parsers.or(
		term("=").retn(Predicate.Type.EQ),
		term("<").retn(Predicate.Type.LS),
		term(">").retn(Predicate.Type.GT),
		term("<=").retn(Predicate.Type.LEQ),
		term(">=").retn(Predicate.Type.GEQ),
		term("!=").retn(Predicate.Type.NEQ),
		term("<>").retn(Predicate.Type.NEQ)
	);

	final Parser<InExpression> IN_EXPR = Parsers.sequence(FIELD, term("in"), CONSTANT_LIST, (field, kw, constants) -> new InExpression(field, constants));

	final Parser<LikeExpression> LIKE_EXPR = Parsers.sequence(FIELD, term("like"), STRING_CONSTANT, (field, kw, regExp) -> new LikeExpression(field, regExp));

	final Parser<IlikeExpression> ILIKE_EXPR = Parsers.sequence(FIELD, term("ilike"), STRING_CONSTANT, (field, kw, regExp) -> new IlikeExpression(field, regExp));

	final Parser<Predicate> COMPARE_EXPR = Parsers.sequence(FIELD, CompOp, CONSTANT, (lhs, type, rhs) -> new Predicate(lhs, rhs, type));

	final Parser<Expression> PREDICATE = Parsers.or(COMPARE_EXPR, LIKE_EXPR, IN_EXPR, ILIKE_EXPR);

	final UnaryOperator<Expression> NOT = (expr) -> new LogicExpression(expr, null, LogicExpression.Type.NOT);
	final BinaryOperator<Expression> AND = (lhs, rhs) -> new LogicExpression(lhs, rhs, LogicExpression.Type.AND);
	final BinaryOperator<Expression> OR = (lhs, rhs) -> new LogicExpression(lhs, rhs, LogicExpression.Type.OR);

	final Parser.Reference<Expression> QUERY_REF = Parser.newReference();

	Parser<Expression> LogicalExpr(Parser<Expression> predicate)
	{
		Parser.Reference<Expression> ref = Parser.newReference();

		Parser<Expression> parser = new OperatorTable<Expression>()
				.prefix(term("not").retn(NOT), 30)
				.infixl(term("and").retn(AND), 20)
				.infixl(term("or").retn(OR),   10)
				.build(Parsers.between(term("("), ref.lazy(), term(")")).or(predicate));

		ref.set(parser);

		return parser;
	}

	final Parser<Expression> QUERY = LogicalExpr(PREDICATE);

	{
		QUERY_REF.set(QUERY);
	}

	final Parser<Expression> GRAMMAR = QUERY.or(Parsers.EOF.retn(new Empty()));

	public Expression parse(String input) throws QuerySyntaxError
	{
		try
		{
			return GRAMMAR.from(TOKENIZER, Scanners.SQL_DELIMITER).parse(input);
		}
		catch (Exception pe)
		{
			throw new QuerySyntaxError(pe);
		}
	}
}
