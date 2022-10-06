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

package com.proit.app.configuration.cache.ehcache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.persistence.jaxb.JAXBContext;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class EhCacheConfigParser
{
	@Getter
	public static class Cache
	{
		@XmlAttribute
		private String name;

		@XmlAttribute
		private int maxEntriesLocalHeap;

		@XmlAttribute
		private int timeToLiveSeconds;

		@XmlAttribute
		private int timeToIdleSeconds;
	}

	@Getter
	@XmlRootElement(name = "ehcache")
	public static class CacheConfig
	{
		@XmlElement(name = "cache")
		private List<Cache> caches = new ArrayList<>();
	}

	public static Optional<CacheConfig> parse()
	{
		try
		{
			var configStream = EhCacheConfigParser.class.getResourceAsStream("/ehcache.xml");

			if (configStream == null)
			{
				return Optional.empty();
			}

			var jaxbContext = JAXBContext.newInstance(CacheConfig.class);
			var unmarshaller = jaxbContext.createUnmarshaller();

			return Optional.ofNullable((CacheConfig) unmarshaller.unmarshal(configStream));
		}
		catch (Exception e)
		{
			log.error("При загрузке конфигурации ehcache произошла ошибка.", e);

			return Optional.empty();
		}
	}
}
