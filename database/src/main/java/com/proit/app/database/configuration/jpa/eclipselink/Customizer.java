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

package com.proit.app.database.configuration.jpa.eclipselink;

import com.proit.app.configuration.properties.AppProperties;
import com.proit.app.database.configuration.jpa.eclipselink.annotation.ReadOnlyFieldAnnotationExtension;
import com.proit.app.database.configuration.jpa.eclipselink.postgis.GeolatteExtension;
import com.proit.app.database.configuration.jpa.eclipselink.schema.SpringAwareTableSchemaResolvingExtension;
import com.proit.app.database.configuration.jpa.eclipselink.uuid.UuidSequence;
import com.proit.app.database.configuration.jpa.eclipselink.uuid.UuidSupportExtension;
import com.proit.app.database.configuration.properties.JPAProperties;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.persistence.config.SessionCustomizer;
import org.eclipse.persistence.sessions.Session;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
public class Customizer implements SessionCustomizer, ApplicationContextAware
{
	private static AppProperties appProperties;
	private static JPAProperties jpaProperties;
	private static ConfigurableBeanFactory beanFactory;

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
	{
		appProperties = applicationContext.getBean(AppProperties.class);
		jpaProperties = applicationContext.getBean(JPAProperties.class);
		beanFactory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
	}

	public void customize(Session session)
	{
		var eventManager = session.getEventManager();

		if (jpaProperties.isPostgisEnabled())
		{
			eventManager.addListener(new GeolatteExtension());
		}

		session.getLogin().addSequence(new UuidSequence("system-uuid"));
		eventManager.addListener(new ReadOnlyFieldAnnotationExtension());
		eventManager.addListener(new SpringAwareTableSchemaResolvingExtension(beanFactory, appProperties));
		eventManager.addListener(new UuidSupportExtension());

		var defaultScheme = jpaProperties.getDefaultScheme();

		if (defaultScheme != null)
		{
			log.info("Default JPA database scheme: {}.", defaultScheme);

			session.getLogin().setTableQualifier(defaultScheme);
		}
	}
}
