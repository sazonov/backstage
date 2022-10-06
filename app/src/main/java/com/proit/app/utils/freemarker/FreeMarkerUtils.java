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

package com.proit.app.utils.freemarker;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.util.Map;

@Component
@ConditionalOnClass(name = "freemarker.template.Configuration")
public class FreeMarkerUtils implements ApplicationContextAware
{
	private static Configuration templateConfiguration;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
	{
		try
		{
			templateConfiguration = applicationContext.getBean(Configuration.class);
		}
		catch (Exception unused)
		{
		}
	}

	public static String processString(String string, Map<String, Object> model)
	{
		final String TEMPORARY_TEMPLATE = "template";

		StringTemplateLoader loader = new StringTemplateLoader();
		loader.putTemplate(TEMPORARY_TEMPLATE, string);

		Configuration configuration = (Configuration) templateConfiguration.clone();
		configuration.setTemplateLoader(loader);

		try
		{
			return FreeMarkerTemplateUtils.processTemplateIntoString(configuration.getTemplate(TEMPORARY_TEMPLATE), model);
		}
		catch (TemplateException | IOException e)
		{
			throw new RuntimeException("failed to process FreeMarker string", e);
		}
	}

	public static String processTemplate(String templateName, Map<String, Object> model)
	{
		if (templateConfiguration == null)
		{
			throw new RuntimeException("there is no active FreeMarker configuration");
		}

		try
		{
			return FreeMarkerTemplateUtils.processTemplateIntoString(templateConfiguration.getTemplate(templateName + ".ftl"), model).trim();
		}
		catch (Exception e)
		{
			throw new RuntimeException("failed to process FreeMarker template", e);
		}
	}
}
