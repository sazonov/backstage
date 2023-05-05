/*
 *    Copyright 2019-2023 the original author or authors.
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

package com.proit.app.service.query.ast;

import com.proit.app.service.query.TranslationContext;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Predicate implements QueryExpression
{
	public enum Type
	{
		EQ,
		NEQ,
		LS,
		GT,
		LEQ,
		GEQ
	}

	public final Field left;
	public final QueryOperand right;
	public final Type type;

	public <T> T process(Visitor<T> visitor, TranslationContext context)
	{
		return visitor.visit(this, context);
	}
}
