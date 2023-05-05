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

package com.proit.app.configuration.cache.ehcache;

import com.proit.app.configuration.cache.distributed.DistributedCacheService;
import com.proit.app.configuration.properties.CacheProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.util.Optional;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(value = CacheProperties.ACTIVATION_PROPERTY, matchIfMissing = true)
@ConditionalOnClass(name = "net.sf.ehcache.CacheManager")
@RequiredArgsConstructor
public class EhCacheCacheConfiguration
{
	private final CacheProperties cacheProperties;

	@Bean
	public CacheProperties cacheProperties()
	{
		return cacheProperties;
	}

	@Bean
	public CacheManager cacheManager(Optional<DistributedCacheService> distributedCacheService)
	{
		EhCacheCacheManager cacheManager = new EnhancedEhCacheCacheManager(distributedCacheService.orElse(null), cacheProperties);
		cacheManager.setCacheManager(ehCacheManagerFactoryBean().getObject());

		return cacheManager;
	}

	@Bean
	public KeyGenerator keyGenerator()
	{
		return new SimpleKeyGenerator();
	}

	@Bean
	public EhCacheManagerFactoryBean ehCacheManagerFactoryBean()
	{
		EhCacheManagerFactoryBean ehCacheManagerFactoryBean = new EhCacheManagerFactoryBean();
		ehCacheManagerFactoryBean.setConfigLocation(new ClassPathResource("ehcache.xml"));
		ehCacheManagerFactoryBean.setCacheManagerName("cacheManager");
		ehCacheManagerFactoryBean.setShared(true);

		return ehCacheManagerFactoryBean;
	}
}
