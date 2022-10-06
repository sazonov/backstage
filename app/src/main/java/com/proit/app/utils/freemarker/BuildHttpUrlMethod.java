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

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
@ConditionalOnClass(name = "freemarker.template.Configuration")
public class BuildHttpUrlMethod implements TemplateMethodModelEx, ModelMethod
{
	public Object exec(List arguments) throws TemplateModelException
	{
		if (arguments.isEmpty())
		{
			throw new TemplateModelException("no arguments specified");
		}

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(arguments.get(0).toString());

		for (int i = 1; i < arguments.size(); i++)
		{
			String[] paramParts = arguments.get(i).toString().split("=");

			if (paramParts.length < 2)
			{
				throw new TemplateModelException(String.format("bad argument at position %d", i + 1));
			}

			builder.queryParam(paramParts[0], paramParts[1]);
		}

		return new SimpleScalar(builder.build().toString());
	}
}
