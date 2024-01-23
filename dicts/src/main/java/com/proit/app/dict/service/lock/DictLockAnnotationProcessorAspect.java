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

package com.proit.app.dict.service.lock;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Аспект для обработки блокирующих аннотаций для работы с данными справочников.
 * @see LockDictOperation
 * @see LockDictSchemaModifyOperation
 */
@ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
@Aspect
@Component
@RequiredArgsConstructor
public class DictLockAnnotationProcessorAspect
{
	private final DictLockService dictLockService;

	//todo: BACKSTAGE-48 обобщить методы обработки аннотаций
	@Around("@annotation(lockDictOperation)")
	public Object processLockDictOperation(ProceedingJoinPoint joinPoint, LockDictOperation lockDictOperation)
	{
		var keyArgument = Objects.requireNonNull(AnnotationUtils.getAnnotation(lockDictOperation, LockDictOperation.class))
				.key();
		var key = (String) getMethodArgumentFromExpression(joinPoint, keyArgument);

		return dictLockService.lockDictOperation(key, joinPoint::proceed);
	}

	@Around("@annotation(lockDictSchemaModifyOperation)")
	public Object processLockDictSchemaModifyOperation(ProceedingJoinPoint joinPoint, LockDictSchemaModifyOperation lockDictSchemaModifyOperation)
	{
		var keyArgument = Objects.requireNonNull(AnnotationUtils.getAnnotation(lockDictSchemaModifyOperation, LockDictSchemaModifyOperation.class))
				.key();
		var key = (String) getMethodArgumentFromExpression(joinPoint, keyArgument);

		return dictLockService.lockDictSchemaModifyOperation(key, joinPoint::proceed);
	}

	/**
	 * Метод для определения значения аругмента с использованием SpEL.
	 */
	private Object getMethodArgumentFromExpression(ProceedingJoinPoint joinPoint, String expression)
	{
		var parser = new SpelExpressionParser();

		var context = new StandardEvaluationContext();
		var parameterNames = ((CodeSignature) joinPoint.getSignature()).getParameterNames();
		var args = joinPoint.getArgs();

		for (var i = 0; i < parameterNames.length; i++)
		{
			context.setVariable(parameterNames[i], args[i]);
		}

		return parser.parseExpression(expression)
				.getValue(context);
	}
}