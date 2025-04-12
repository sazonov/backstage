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

package com.proit.app.dict.endpoint.remote;

import com.proit.app.dict.api.model.dto.data.DictItemRemoteDto;
import com.proit.app.dict.api.service.remote.RemoteDictDataService;
import com.proit.app.dict.conversion.dto.DictConverter;
import com.proit.app.dict.conversion.dto.data.DictItemConverter;
import com.proit.app.dict.endpoint.GenericDictDataEndpoint;
import com.proit.app.dict.service.DictDataService;
import com.proit.app.dict.service.DictService;
import com.proit.app.dict.service.export.DictExportService;
import com.proit.app.dict.service.imp.ImportCsvService;
import com.proit.app.dict.service.imp.ImportJsonService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/remote/dicts")
@Tag(name = "remote-dict-data-endpoint", description = "Методы для работы с данными справочников (Remote)")
public class RemoteDictDataEndpoint extends GenericDictDataEndpoint<DictItemRemoteDto> implements RemoteDictDataService
{
	public RemoteDictDataEndpoint(DictService dictService,
	                              DictDataService dictDataService,
	                              ImportCsvService importCsvService,
	                              ImportJsonService importJsonService,
	                              DictExportService dictExportService,
	                              DictConverter dictConverter,
	                              DictItemConverter dictItemConverter)
	{
		super(dictService, dictDataService, importCsvService, importJsonService, dictExportService, dictConverter, dictItemConverter);
	}
}
