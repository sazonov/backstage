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

public interface Visitor<T>
{
	T visit(Constant constant, TranslationContext context);

	T visit(Field field, TranslationContext context);

	T visit(LikeQueryExpression expression, TranslationContext context);

	T visit(IlikeQueryExpression expression, TranslationContext context);

	T visit(LogicQueryExpression expression, TranslationContext context);

	T visit(Predicate predicate, TranslationContext context);

	T visit(InQueryExpression expression, TranslationContext context);

	T visit(Empty empty, TranslationContext context);
}
