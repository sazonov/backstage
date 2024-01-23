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

package com.proit.app.model.other.exception;

import lombok.Getter;

@Getter
public enum ApiStatusCodeImpl implements ApiStatusCode
{
	OK(0, "Операция выполнена успешно.", ApiStatusCategory.SUCCESS),
	UNKNOWN_ERROR(1, "Неизвестная ошибка."),
	ACCESS_RIGHTS_ERROR(2, "Ошибка прав доступа.", ApiStatusCategory.ACCESS_RIGHTS),
	ILLEGAL_INPUT(3, "Некорректные входные данные."),
	ILLEGAL_DATA_FORMAT(4, "Некорректный формат данных."),
	OBJECT_NOT_FOUND(5, "Указанный объект не найден.", ApiStatusCategory.NOT_FOUND),
	CAPTCHA_CHECK_ERROR(6, "Ошибка проверки капчи.", ApiStatusCategory.ACCESS_RIGHTS),

	SERIALIZE_ERROR(30, "Ошибка сериализации обьекта."),
	DESERIALIZE_ERROR(31, "Ошибка десериализации обьекта."),

	ATTACHMENT_ADD_ERROR(100, "Невозможно сохранить вложение."),
	ATTACHMENT_DELETE_ERROR(101, "Невозможно удалить вложение."),
	ATTACHMENT_TYPE_NOT_SUPPORTED(102, "Данный тип вложения не поддерживается."),
	ATTACHMENT_INVALID_CONTENT(103, "Содержимое вложения не соответствует типу."),
	ATTACHMENT_NOT_FOUND(104, "Вложение не найдено.", ApiStatusCategory.NOT_FOUND),
	ATTACHMENT_DATA_NOT_AVAILABLE(105, "Данные вложения не доступны.", ApiStatusCategory.NOT_FOUND),
	ATTACHMENT_STORE_INIT_FAILED(106, "Ошибка при инициализации хранилища вложений."),
	ATTACHMENT_STORE_ERROR(107, "При обращении к хранилищу вложений произошла ошибка."),
	ATTACHMENT_STORE_SYNC_ERROR(108, "Ошибка при синхронизации вложений между хранилищами."),

	// TODO: потеряли логику присвоения кодов ниже.
	DOCUMENT_TEMPLATE_GENERATE_ERROR(109, "Ошибка при создании печатной формы по шаблону."),

	EMAIL_SEND_ERROR(200, "Ошибка отправки email сообщения."),

	DICTS_ERROR(300, "При обращении к справочникам произошла ошибка."),

	REPORT_GENERATE_ERROR(400, "При генерации отчета произошла ошибка."),

	DATE_PARSE_ERROR(500, "Неправильный формат даты."),

	REMOTE_SERVICE_ERROR(600, "При обращении к сервису произошла ошибка.");

	private final Integer code;

	private final String message;

	private final ApiStatusCategory category;

	ApiStatusCodeImpl(Integer code, String message)
	{
		this(code, message, ApiStatusCategory.OTHER);
	}

	ApiStatusCodeImpl(Integer code, String message, ApiStatusCategory category)
	{
		this.code = code;
		this.message = message.isEmpty() ? this.toString() : message;
		this.category = category;
	}
}
