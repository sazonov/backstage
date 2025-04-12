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

package com.proit.bpm.service.script;

import com.proit.bpm.model.WorkflowScript;

public interface ScriptBackend
{
	/**
	 * Возвращает true, если бэкенд поддерживает переданный тип скриптов.
 	 */
	boolean supports(WorkflowScript.Type type);

	/**
	 * Компилирует скрипт и возвращает соответствующий объект.
 	 */
	Script compile(WorkflowScript workflowScript);

	/**
	 * Выполняет указанный скрипт.
	 */
	Object execute(Script script, String methodName, Object... parameters);
}
