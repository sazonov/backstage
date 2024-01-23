package com.proit.feign.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("app.feign")
public class FeignSessionProperties
{
	public static final String GLOBAL_INTERCEPTOR_ACTIVATION_PROPERTY = "app.feign.propagate-request-session";

	/**
	 * Активирует обработку пользовательской сессии во входящих feign запросах.
	 */
	private boolean propagateRequestSession = true;
}
