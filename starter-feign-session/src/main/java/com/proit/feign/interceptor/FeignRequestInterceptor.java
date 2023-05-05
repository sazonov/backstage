package com.proit.feign.interceptor;

import com.proit.app.utils.SecurityUtils;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

@Component
public class FeignRequestInterceptor implements RequestInterceptor
{
	@Override
	public void apply(RequestTemplate template)
	{
		String currentUserId = SecurityUtils.getCurrentUserId();

		template.header(RequestHeaders.USER_ID.getName(), currentUserId);
	}
}
