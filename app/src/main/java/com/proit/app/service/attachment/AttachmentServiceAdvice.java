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

package com.proit.app.service.attachment;

import java.util.List;

/**
 * Класс, который может быть использован для уточнения основных операций {@link AttachmentService}.
 */
public interface AttachmentServiceAdvice
{
	/**
	 * Вызывается во время попытки скачивания вложения. Может быть использован для контроля прав доступа.
	 * @param id идентификатор запрашиваемого вложения
	 */
	default void handleGetAttachmentData(String id)
	{
	}

	/**
	 * Вызывается во время попытки доступа к данным вложения (не загрузке). Может быть использован для контроля прав доступа.
	 * @param id идентификатор запрашиваемого вложения
	 */
	default void handleGetAttachment(String id)
	{
	}

	/**
	 * Вызывается во время попытки доступа к данным вложений (не загрузке). Может быть использован для контроля прав доступа.
	 * @param ids коллекция идентификаторов запрашиваемых вложений
	 */
	default void handleGetAttachments(List<String> ids)
	{
	}

	/**
	 * Вызывается во время добавления нового вложения в систему. Может быть использован для контроля прав и
	 * содержимого вложения.
	 * @param id желаемый идентификатор вложения, если не указан, присваивается автоматически
	 * @param fileName название файла
	 * @param mimeType MIME-тип
	 * @param userId идентификатор владельца вложения
	 * @param data данные вложения
	 */
	default void handleAddAttachment(String id, String fileName, String mimeType, String userId, byte[] data)
	{
	}

	/**
	 * Вызывается во время связывания объекта системы с вложением. Метод может быть использован для контроля прав
	 * доступа к указанному вложению.
	 * @param attachmentId идентификатор вложения
	 * @param userId идентификатор пользователя, который получает доступ к вложению
	 * @param type тип вложения
	 * @param objectId идентификатор объекта системы, с которым связывается вложение
	 */
	default void handleBindAttachment(String attachmentId, String userId, String type, String objectId)
	{
	}
}
