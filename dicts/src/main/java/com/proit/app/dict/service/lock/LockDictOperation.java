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

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Аннотация используется над методами, которые должны быть заблокированы во время изменения схемы справочника.
 * <p>Возможен deadlock, если внутри операции чтения в рамках одного потока будет попытка вызова операции изменения справочника.</p>
 * @see java.util.concurrent.locks.ReentrantReadWriteLock
 * @see DictLockAnnotationProcessorAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LockDictOperation
{
	/**
	 * Ключ, по которому будет происходить блокировка.
	 * <p>Поддерживает динамические определение через Spring Expression Language (SpEL).</p>
	 * Пример:
	 * <pre>{@code
	 *  @LockDictOperation("#id")
	 * 	public Dict getById(String id)
	 * 	{
	 * 	    //read operation here
	 * 	}
	 * }</pre>
	 */

	String value() default "";

	@AliasFor("value")
	String key() default "";
}