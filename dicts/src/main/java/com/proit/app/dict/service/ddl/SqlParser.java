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

package com.proit.app.dict.service.ddl;

import com.proit.app.dict.exception.DDLSyntaxException;
import com.proit.app.dict.service.ddl.ast.*;
import com.proit.app.dict.service.ddl.ast.expression.*;
import com.proit.app.dict.service.ddl.ast.expression.table.AlterTable;
import com.proit.app.dict.service.ddl.ast.expression.table.CreateIndexExpression;
import com.proit.app.dict.service.ddl.ast.expression.table.CreateTable;
import com.proit.app.dict.service.ddl.ast.expression.table.DeleteIndexExpression;
import com.proit.app.dict.service.ddl.ast.expression.table.operation.*;
import com.proit.app.dict.service.ddl.ast.value.*;
import com.proit.app.dict.service.query.ast.Predicate;
import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;

import java.util.List;

public class SqlParser
{
	static final String[] KEYWORDS = {
			"insert", "into", "values",
			"update", "set", "where",
			"create", "table", "index", "unique", "on", "not", "null", "references", "constraint",
			"alter", "add", "drop", "column", "rename", "to",
			"delete", "from",
			"true", "false",
			"array", "asc", "desc",
			"for", "as", "enum", "value",
			"engine"
	};

	static final String[] OPERATORS = {
			"=", "!=", "<", ">", "<=", ">=", ".", "*", ",", "(", ")", "+", "-", "/", ";", "[]", "[", "]", "\"", "<>", "::"
	};

	final Terminals TERMS = Terminals.operators(OPERATORS).words(Scanners.IDENTIFIER).caseInsensitiveKeywords(KEYWORDS).build();

	final Parser<?> TOKENIZER = Parsers.or(Terminals.DecimalLiteral.TOKENIZER, Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER, TERMS.tokenizer());

	Parser<?> term(String term)
	{
		return TERMS.token(term);
	}

	Parser<?> phrase(String... terms)
	{
		return TERMS.phrase(terms);
	}

	final Parser<DecimalValue> DECIMAL_VALUE = Terminals.DecimalLiteral.PARSER.map(str -> new DecimalValue(Double.parseDouble(str)));

	final Parser<StringValue> STRING_VALUE = Terminals.StringLiteral.PARSER.map(StringValue::new);

	final Parser<BooleanValue> BOOLEAN_VALUE = Parsers.or(
			term("true").retn(new BooleanValue(true)),
			term("false").retn(new BooleanValue(false))
	);

	final Parser<Value<Void>> NULL_VALUE = term("null").retn(new NullValue());

	final Parser<Value<?>> COLUMN_VALUE = Terminals.Identifier.PARSER.map(Id::new).map(ColumnValue::new);

	final Parser<JsonValue> JSON_VALUE = Parsers.sequence(STRING_VALUE, term("::"), Terminals.Identifier.PARSER, (value, kw, targetType) -> new JsonValue(value.getValue()));

	final Parser<Id> ID = Parsers.or(Terminals.Identifier.PARSER, Terminals.Identifier.PARSER.between(term("\""), term("\""))).map(Id::new);
	final Parser<IdWithName> ID_WITH_NAME = Parsers.sequence(ID, STRING_VALUE.between(term("["), term("]")).optional(new StringValue(null)), IdWithName::new);

	final Parser<ArrayValue> ARRAY_VALUE = Parsers.sequence(
			term("array"),
			Parsers.or(JSON_VALUE, DECIMAL_VALUE, STRING_VALUE, BOOLEAN_VALUE, NULL_VALUE).sepBy1(term(","))
					.between(term("["), term("]"))
					.optional(List.of()),
			(t, p) -> new ArrayValue(p));

	final Parser<Value<?>> VALUE = Parsers.or(JSON_VALUE, DECIMAL_VALUE, STRING_VALUE, BOOLEAN_VALUE, NULL_VALUE, ARRAY_VALUE);

	final Parser<Expression> CREATE_ENUM_EXPR = Parsers.sequence(
			phrase("create", "enum").next(ID),
			STRING_VALUE.between(term("["), term("]")).optional(new StringValue(null)),
			term("for").next(ID),
			term("as").next(STRING_VALUE.sepBy1(term(",")).between(term("("), term(")"))),
			CreateEnum::new);

	final Parser<ColumnDefinition> COLUMN_DEFINITION = Parsers.sequence(
			ID,
			STRING_VALUE.between(term("["), term("]")).optional(new StringValue(null)),
			ID,
			term("[]").asOptional(), phrase("not", "null").asOptional(), Parsers.sequence(term("references"), ID).asOptional(),
			(field, name, type, multivalued, required, reference) -> new ColumnDefinition(field, name, type, multivalued.isPresent(), required.isPresent(), reference.orElse(null)));

	final Parser<Expression> CREATE_TABLE_EXPR = Parsers.sequence(
			phrase("create", "table").next(ID),
			STRING_VALUE.between(term("["), term("]")).optional(new StringValue(null)),
			COLUMN_DEFINITION.sepBy1(term(",")).between(term("("), term(")")),
			phrase("engine", "=").next(STRING_VALUE).optional(new StringValue(null)),
			CreateTable::new);

	final Parser<Expression> CREATE_INDEX_EXPR = Parsers.sequence(
			phrase("create", "index").next(ID),
			phrase("on").next(ID),
			ID.sepBy1(term(",")).between(term("("), term(")")).optional(List.of()),
			phrase("desc").asOptional(),
			(id, table, fields, descDirection) -> new CreateIndexExpression(id, table, descDirection.isPresent(), fields));

	final Parser<Expression> DROP_INDEX_EXPR = Parsers.sequence(
			phrase("drop", "index").next(ID),
			phrase("on").next(ID),
			DeleteIndexExpression::new);

	final Parser<AddTableColumnOperation> ADD_TABLE_COLUMN_EXPR = Parsers.sequence(
			phrase("add", "column"), COLUMN_DEFINITION).map(AddTableColumnOperation::new);

	final Parser<DropEnumOperation> DROP_ENUM_EXPR = phrase("drop", "enum").next(ID)
			.map(DropEnumOperation::new);

	final Parser<AddEnumValueOperation> ADD_ENUM_VALUE_EXPR = Parsers.sequence(
			phrase("alter", "enum").next(ID),
			phrase("add", "value").next(STRING_VALUE),
			AddEnumValueOperation::new);

	final Parser<DropTableColumnOperation> DROP_TABLE_COLUMN_EXPR = Parsers.sequence(
			phrase("drop", "column"), ID).map(DropTableColumnOperation::new);

	final Parser<AddConstraintColumnOperation> ADD_TABLE_CONSTRAINT_EXPR = Parsers.sequence(
			phrase("add", "constraint").next(ID),
			term("unique").next(ID.sepBy1(term(",")).between(term("("), term(")")).optional(List.of())),
			AddConstraintColumnOperation::new);

	final Parser<DropConstraintColumnOperation> DROP_TABLE_CONSTRAINT_EXPR = phrase("drop", "constraint").next(ID)
			.map(DropConstraintColumnOperation::new);

	final Parser<TableParameter> TABLE_PARAMETER = ID.map(id -> TableParameter.fromString(id.getName()));

	final Parser<SetTableParameterOperation> ALTER_TABLE_PARAMETER_EXPR = Parsers.sequence(
			term("set").next(TABLE_PARAMETER),
			term("=").next(Parsers.or(STRING_VALUE, NULL_VALUE.map((v) -> new StringValue(null)))),
			SetTableParameterOperation::new);

	final Parser<RenameColumnOperation> RENAME_COLUMN_EXPR = Parsers.sequence(
			phrase("rename", "column").next(ID),
			term("to").next(ID_WITH_NAME),
			RenameColumnOperation::new);

	final Parser<Expression> ALTER_TABLE_EXPR = Parsers.sequence(
			phrase("alter", "table").next(ID),
			Parsers.or(
					ADD_TABLE_COLUMN_EXPR, DROP_TABLE_COLUMN_EXPR, ALTER_TABLE_PARAMETER_EXPR,
					RENAME_COLUMN_EXPR, ADD_TABLE_CONSTRAINT_EXPR, DROP_TABLE_CONSTRAINT_EXPR,
					ADD_ENUM_VALUE_EXPR, DROP_ENUM_EXPR),
			AlterTable::new);

	final Parser<Expression> INSERT_EXPR = Parsers.sequence(
			phrase("insert", "into").next(ID),
			ID.sepBy1(term(",")).between(term("("), term(")")).optional(List.of()),
			term("values"),
			VALUE.sepBy1(term(",")).between(term("("), term(")")).sepBy1(term(",")),
			(table, fields, term, values) -> new Insert(table, fields, values));

	final Parser<?> COMPARING_TERMS = Parsers.or(term("="), term("!="),
			term("<"), term(">"),
			term("<="), term(">="));

	final Parser<Predicate.Type> CompOp = Parsers.or(
			term("=").retn(Predicate.Type.EQ),
			term("<").retn(Predicate.Type.LS),
			term(">").retn(Predicate.Type.GT),
			term("<=").retn(Predicate.Type.LEQ),
			term(">=").retn(Predicate.Type.GEQ),
			term("!=").retn(Predicate.Type.NEQ),
			term("<>").retn(Predicate.Type.NEQ)
	);

	final Parser<ColumnWithValue> COLUMN_VALUE_SET_EXPR = Parsers.sequence(
			ID,
			COMPARING_TERMS.next(Parsers.or(COLUMN_VALUE, VALUE)),
			ColumnWithValue::new);

	final Parser<ComparingValueColumn> COMPARING_VALUE_EXPR = Parsers.sequence(
			ID,
			CompOp,
			Parsers.or(COLUMN_VALUE, VALUE),
			ComparingValueColumn::new);

	final Parser<Expression> UPDATE_EXPR = Parsers.sequence(
			term("update").next(ID),
			term("set").next(COLUMN_VALUE_SET_EXPR.sepBy1(term(","))),
			term("where").next(COMPARING_VALUE_EXPR).optional(null),
			Update::new);

	final Parser<Expression> DELETE_EXPR = Parsers.sequence(
			phrase("delete", "from").next(ID),
			term("where").next(COMPARING_VALUE_EXPR).optional(null),
			Delete::new);

	final Parser<Expression> DROP_EXPR = phrase("drop", "table").next(ID).map(Drop::new);

	final Parser<List<Expression>> EXPRESSIONS = Parsers.or(
					CREATE_TABLE_EXPR, ALTER_TABLE_EXPR, INSERT_EXPR, UPDATE_EXPR, DELETE_EXPR, DROP_EXPR, CREATE_INDEX_EXPR,
					DROP_INDEX_EXPR, CREATE_ENUM_EXPR)
			.followedBy(term(";")).atLeast(1);

	public List<Expression> parse(String input)
	{
		try
		{
			return EXPRESSIONS.or(Parsers.EOF.retn(List.of())).from(TOKENIZER, Scanners.SQL_DELIMITER).parse(input);
		}
		catch (Exception e)
		{
			throw new DDLSyntaxException(input, e);
		}
	}
}
