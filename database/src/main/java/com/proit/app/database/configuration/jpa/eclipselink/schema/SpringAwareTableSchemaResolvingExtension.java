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

package com.proit.app.database.configuration.jpa.eclipselink.schema;

import com.proit.app.configuration.properties.AppProperties;
import com.proit.app.utils.SpringContextUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.internal.helper.DatabaseTable;
import org.eclipse.persistence.mappings.DirectCollectionMapping;
import org.eclipse.persistence.mappings.ManyToManyMapping;
import org.eclipse.persistence.sessions.SessionEvent;
import org.eclipse.persistence.sessions.SessionEventAdapter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

@RequiredArgsConstructor
public class SpringAwareTableSchemaResolvingExtension extends SessionEventAdapter
{
	private static final String PLACEHOLDER_REGEX = "^\\$\\{([\\w.]+)}$";
	private static final String SPEL_TEMPLATE_REGEX = "^#\\{(.+)}$";

	private final ConfigurableBeanFactory beanFactory;

	private final AppProperties appProperties;

	@Override
	public void preLogin(final SessionEvent event)
	{
		event.getSession()
				.getDescriptors()
				.forEach((clazz, descriptor) -> {

					if (descriptor.getTables().isEmpty())
					{
						return;
					}

					descriptor.getMappings()
							.stream()
							.filter(it -> it instanceof ManyToManyMapping)
							.map(ManyToManyMapping.class::cast)
							.map(ManyToManyMapping::getRelationTable)
							.forEach(this::resolveTableScheme);

					descriptor.getMappings()
							.stream()
							.filter(it -> it instanceof DirectCollectionMapping)
							.map(DirectCollectionMapping.class::cast)
							.map(DirectCollectionMapping::getReferenceTable)
							.forEach(this::resolveTableScheme);

					descriptor.getTables()
							.forEach(this::resolveTableScheme);
				});
	}

	private void resolveTableScheme(DatabaseTable table)
	{
		var schemaValue = table.getTableQualifier();
		var schema = schemaValue;

		if (schemaValue.matches(PLACEHOLDER_REGEX))
		{
			var resolvedSchema = beanFactory.resolveEmbeddedValue(schemaValue);

			schema = schemaValue.equals(resolvedSchema)
					? StringUtils.EMPTY
					: resolvedSchema;
		}
		else if (schemaValue.matches(SPEL_TEMPLATE_REGEX))
		{
			var resolvedSchema = getValueFromExpression(schemaValue);

			schema = resolvedSchema == null
					? StringUtils.EMPTY
					: resolvedSchema;
		}

		table.setTableQualifier(schema);
	}

	private String getValueFromExpression(String expression)
	{
		var parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setBeanResolver(new BeanFactoryResolver(SpringContextUtils.exposeConfigurationProperties(appProperties.getBasePackages(), beanFactory)));

		return parser.parseExpression(expression, new TemplateParserContext())
				.getValue(context, String.class);
	}
}
