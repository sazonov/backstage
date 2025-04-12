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

package com.proit.app.utils;

import lombok.experimental.UtilityClass;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.function.Consumer;

@UtilityClass
public class RestUtils
{
	private static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 60;

	public RestTemplate createTrustfulRestTemplate() throws Exception
	{
		return createTrustfulRestTemplate(DEFAULT_REQUEST_TIMEOUT_SECONDS);
	}

	public RestTemplate createTrustfulRestTemplate(int requestTimeoutSeconds) throws Exception
	{
		return createTrustfulRestTemplate(requestTimeoutSeconds, null);
	}

	public RestTemplate createTrustfulRestTemplate(int requestTimeoutSeconds, int maxConnectionPoolSize, int maxConnectionPerRoute) throws Exception
	{
		return createRestTemplate(requestTimeoutSeconds, maxConnectionPoolSize, maxConnectionPerRoute, getSslConfigurator());
	}

	public RestTemplate createTrustfulRestTemplate(int requestTimeoutSeconds, Consumer<HttpClientBuilder> httpClientConfigurator) throws Exception
	{
		var sslConfigurator = getSslConfigurator();

		return createRestTemplate(requestTimeoutSeconds, httpClientConfigurator == null ? sslConfigurator : httpClientConfigurator.andThen(sslConfigurator));
	}

	public RestTemplate createRestTemplate(int requestTimeoutSeconds)
	{
		return createRestTemplate(requestTimeoutSeconds, null);
	}

	public RestTemplate createRestTemplate(int requestTimeoutSeconds, Consumer<HttpClientBuilder> httpClientConfigurator)
	{
		return createRestTemplate(requestTimeoutSeconds, 0, 0, httpClientConfigurator);
	}

	public RestTemplate createRestTemplate(int requestTimeoutSeconds, int maxConnectionPoolSize, int maxConnectionPerRoute, Consumer<HttpClientBuilder> httpClientConfigurator)
	{
		var httpClientBuilder = HttpClients.custom()
				.setMaxConnTotal(maxConnectionPoolSize)
				.setMaxConnPerRoute(maxConnectionPerRoute)
				.setDefaultRequestConfig(getRequestConfig(requestTimeoutSeconds));

		if (httpClientConfigurator != null)
		{
			httpClientConfigurator.accept(httpClientBuilder);
		}

		var httpClient = httpClientBuilder.build();
		var httpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

		var restTemplate = new RestTemplate();
		restTemplate.setRequestFactory(httpRequestFactory);

		return restTemplate;
	}

	private Consumer<HttpClientBuilder> getSslConfigurator() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException
	{
		var sslContextBuilder = new SSLContextBuilder();
		sslContextBuilder.loadTrustMaterial(null, new TrustAllStrategy());

		var socketFactory = new SSLConnectionSocketFactory(sslContextBuilder.build(), new NoopHostnameVerifier());

		return builder -> builder.setSSLSocketFactory(socketFactory);
	}

	private RequestConfig getRequestConfig(int requestTimeoutSeconds)
	{
		return RequestConfig.custom()
				.setConnectionRequestTimeout(requestTimeoutSeconds * 1000)
				.setConnectTimeout(requestTimeoutSeconds * 1000)
				.setSocketTimeout(requestTimeoutSeconds * 1000)
				.build();
	}
}
