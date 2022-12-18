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

package com.proit.app.configuration.properties;

import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.MediaType;

import java.util.Set;

@Getter
@Setter
@ConfigurationProperties("app.attachments")
public class AttachmentProperties
{
	public static final String ACTIVATION_PROPERTY = "app.attachments.enabled";

	public static final String IMAGE_BPM_VALUE = "image/bmp";
	public static final String IMAGE_MS_BPM_VALUE = "image/x-ms-bmp";
	public static final String APPLICATION_ZIP = "application/zip";
	public static final String APPLICATION_X_ZIP = "application/x-zip-compressed";
	public static final String APPLICATION_X_RAR = "application/x-rar-compressed";
	public static final String APPLICATION_7_ZIP = "application/x-7z-compressed";
	public static final String APPLICATION_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	public static final String APPLICATION_DOC = "application/msword";
	public static final String APPLICATION_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	public static final String APPLICATION_XLS = "application/vnd.ms-excel";
	public static final String APPLICATION_XLSM = "application/vnd.ms-excel.sheet.macroEnabled.12";

	public enum StoreType
	{
		DIRECTORY,
		MINIO
	}

	@Getter
	@Setter
	public static class MinioProperties
	{
		public static final String ACTIVATION_PROPERTY = "app.attachments.minio.endpoint";

		private String endpoint;

		private String accessKey;

		private String secretKey;

		private String bucket;
	}

	public static class DirectoryProperties
	{
		public static final String ACTIVATION_PROPERTY = "app.attachments.directory.path";
		public static final String DEPRECATED_ACTIVATION_PROPERTY = "app.attachments.store-path";

		private String path;
	}

	/**
	 * Активирует АПИ и сервисы для работы с вложениями.
	 */
	private boolean enabled;

	/**
	 * Тип хранилища для вложений.
	 */
	private StoreType storeType = StoreType.DIRECTORY;

	/**
	 * Путь к папке, в которую будут помещаться загружаемые вложения.
	 * @deprecated Необходимо использовать {@link directory}
	 */
	@Deprecated(forRemoval = true)
	private String storePath;

	/**
	 * Ипользуется, когда в {@link storeType} установлено значение {@link StoreType.DIRECTORY}.
	 * Определяет конфигурацию для хранения вложений в указанной папке.
	 */
	private DirectoryProperties directory;

	/**
	 * Ипользуется, когда в {@link storeType} установлено значение {@link StoreType.MINIO}.
	 * Определяет специфичную конфигурацию для клиента MinIO.
	 */
	private MinioProperties minio;

	/**
	 * Внешний адрес, по которому будет доступна выдача вложений. К этому адресу добавляется адрес метода АПИ
	 * для выдачи вложений в {@link com.proit.app.model.dto.attachment.AttachmentDto#getUrl()}.
	 * Пример: для baseUrl - http://localhost/store, url будет http://localhost/api/attachment/get/{attachment.id}.
	 */
	private String baseUrl;

	/**
	 * Включает проверку mime типа вложения при его загрузку, используется в связке с {@link AttachmentProperties#mimeTypes}.
	 */
	private boolean checkMimeTypes = true;

	/**
	 * Список допустимых для загрузки типов файлов.
	 */
	private Set<String> mimeTypes = Sets.newHashSet(
			IMAGE_BPM_VALUE,
			IMAGE_MS_BPM_VALUE,
			APPLICATION_ZIP,
			APPLICATION_X_ZIP,
			APPLICATION_X_RAR,
			APPLICATION_7_ZIP,
			APPLICATION_DOCX,
			APPLICATION_DOC,
			APPLICATION_XLS,
			APPLICATION_XLSX,
			APPLICATION_XLSM,

			MediaType.IMAGE_JPEG_VALUE,
			MediaType.IMAGE_PNG_VALUE,
			MediaType.APPLICATION_PDF_VALUE);

	/**
	 * Активирует проверку соответствия содержимого файла и Content-Type.
	 */
	private boolean verifyContent;

	/**
	 * Активирует механизм удаления не привязанных вложений.
	 */
	private boolean deleteUnbounded;
}
