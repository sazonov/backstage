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

package com.proit.app.service;

import com.proit.app.exception.AppException;
import com.proit.app.model.api.ApiStatusCodeImpl;
import com.proit.app.model.other.user.UserInfo;
import com.proit.app.service.user.PermissionService;
import com.proit.app.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DictPermissionService
{
	private final DictService dictService;
	private final PermissionService permissionService;

	public void checkViewPermission(String dictId)
	{
		checkViewPermission(dictId, SecurityUtils.getCurrentUserId());
	}


	public void checkViewPermission(String dictId, String userId)
	{
		if (UserInfo.SYSTEM_USER_ID.equals(userId))
		{
			return;
		}

		var permission = dictService.getById(dictId).getViewPermission();

		if (permission == null)
		{
			return;
		}

		if (!permissionService.getPermissions(userId).contains(permission))
		{
			throw new AppException(ApiStatusCodeImpl.ACCESS_RIGHTS_ERROR, "Недоступен просмотр справочника: %s.".formatted(dictId));
		}
	}

	public void checkEditPermission(String dictId)
	{
		checkEditPermission(dictId, SecurityUtils.getCurrentUserId());
	}

	public void checkEditPermission(String dictId, String userId)
	{
		if (UserInfo.SYSTEM_USER_ID.equals(userId))
		{
			return;
		}

		var permission = dictService.getById(dictId).getEditPermission();

		if (permission == null)
		{
			return;
		}
		if (!permissionService.getPermissions(userId).contains(permission))
		{
			throw new AppException(ApiStatusCodeImpl.ACCESS_RIGHTS_ERROR, "Недоступно изменение справочника: %s.".formatted(dictId));
		}
	}
}
