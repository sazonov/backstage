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

package com.proit.app.cache.configuration.ehcache;

import com.proit.app.cache.configuration.distributed.DistributedCacheService;
import com.proit.app.cache.configuration.properties.CacheProperties;
import lombok.RequiredArgsConstructor;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.cache.jcache.JCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Optional;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(value = CacheProperties.ACTIVATION_PROPERTY, matchIfMissing = true)
@ConditionalOnClass(name = "org.ehcache.CacheManager")
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
	public CacheManager cacheManager(Optional<DistributedCacheService> distributedCacheService,
	                                 JCacheManagerFactoryBean cacheManagerFactory)
	{
		var jCacheManager = cacheManagerFactory().getObject();

		EhCacheConfigParser.parse()
				.ifPresent(items -> items.getCaches()
						.forEach(item -> {
							var config = CacheConfigurationBuilder.newCacheConfigurationBuilder(
											Object.class, Object.class, ResourcePoolsBuilder.heap(item.getMaxEntriesLocalHeap()))
									.withExpiry(ExpiryPolicyBuilder.expiry()
											.create(secondsToDuration(item.getTimeToLiveSeconds()))
											.access(secondsToDuration(item.getTimeToIdleSeconds()))
											.update(secondsToDuration(item.getTimeToLiveSeconds()))
											.build()
									);

							jCacheManager.createCache(item.getName(), Eh107Configuration.fromEhcacheCacheConfiguration(config));
						}));

		var cacheManager = new EnhancedEhCacheCacheManager(distributedCacheService.orElse(null), cacheProperties);
		cacheManager.setCacheManager(cacheManagerFactory.getObject());

		return cacheManager;
	}

	@Bean
	public KeyGenerator keyGenerator()
	{
		return new SimpleKeyGenerator();
	}

	@Bean
	public JCacheManagerFactoryBean cacheManagerFactory()
	{
		return new JCacheManagerFactoryBean();
	}

	private Duration secondsToDuration(int seconds)
	{
		return Duration.ofSeconds(seconds == 0 ? Long.MAX_VALUE : seconds);
	}
}
