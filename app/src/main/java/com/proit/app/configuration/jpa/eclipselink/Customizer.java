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

package com.proit.app.configuration.jpa.eclipselink;

import com.proit.app.configuration.jpa.eclipselink.postgis.GeolatteExtension;
import com.proit.app.configuration.properties.JPAProperties;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.persistence.config.SessionCustomizer;
import org.eclipse.persistence.sessions.Session;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

@Slf4j
public class Customizer implements SessionCustomizer, ApplicationContextAware
{
	private static JPAProperties jpaProperties;

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
	{
		jpaProperties = applicationContext.getBean(JPAProperties.class);
	}

	public void customize(Session session)
	{
		if (jpaProperties.isPostgisEnabled())
		{
			session.getEventManager().addListener(new GeolatteExtension());
		}

		session.getLogin().addSequence(new UuidSequence("system-uuid"));

		var defaultScheme = jpaProperties.getDefaultScheme();

		if (defaultScheme != null)
		{
			log.info("Default JPA database scheme: {}.", defaultScheme);

			session.getLogin().setTableQualifier(defaultScheme);
		}
	}
}
