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

package com.proit.app.conversion.dto.mixin;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Stream;

/**
 * Класс реализует механизм кастомизации поведения для конвертеров.
 */
@ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
@Aspect
@Component
public class ConverterMixinAspect
{
	@Autowired(required = false)
	private List<ConverterMixin> converterMixins = Collections.emptyList();

	private Map<Class, List<ConverterMixin>> mixinCache = new HashMap<>();

	@PostConstruct
	public void initialize()
	{
		converterMixins.forEach(converterMixin -> {
			for (var innerType : ResolvableType.forInstance(converterMixin).getInterfaces())
			{
				if (innerType.getRawClass() != null && innerType.getRawClass().isAssignableFrom(ConverterMixin.class))
				{
					Class mixinClass = innerType.getGeneric(0).getRawClass();

					if (!mixinCache.containsKey(mixinClass))
					{
						mixinCache.put(mixinClass, new ArrayList<>());
					}

					mixinCache.get(mixinClass).add(converterMixin);
				}
			}
		});
	}

	@SuppressWarnings("unchecked")
	@AfterReturning(value = "this(com.proit.app.conversion.dto.Converter+) && execution(* *.convert(*))", returning = "result")
	public void applyMixins(JoinPoint joinPoint, Object result)
	{
		if (converterMixins.isEmpty())
		{
			return;
		}

		var mixins = mixinCache.get(joinPoint.getTarget().getClass());

		if (mixins != null && !mixins.isEmpty())
		{
			if (result instanceof List)
			{
				if (!((List) result).isEmpty())
				{
					mixins.forEach(mixin -> mixin.process((List) result));
				}
			}
			else if (result instanceof Page)
			{
				if (!((Page) result).isEmpty())
				{
					mixins.forEach(mixin -> mixin.process(((Page) result).getContent()));
				}
			}
			else if (result instanceof Stream)
			{
				if (((Stream) result).findAny().isPresent())
				{
					mixins.forEach(mixin -> mixin.process(((Stream) result)));
				}
			}
			else if (result.getClass().isArray())
			{
				if (((Object[]) result).length > 0)
				{
					mixins.forEach(mixin -> mixin.process(((Object[]) result)));
				}
			}
			else
			{
				mixins.forEach(mixin -> mixin.process(result));
			}
		}
	}
}
