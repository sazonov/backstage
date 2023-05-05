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

package com.proit.app.configuration.jpa;

import com.proit.app.configuration.ddl.DDLConfiguration;
import com.proit.app.configuration.jpa.customizer.EntityManagerFactoryCustomizer;
import com.proit.app.configuration.jpa.eclipselink.Customizer;
import com.proit.app.configuration.jpa.eclipselink.MetaModelVerifier;
import com.proit.app.configuration.properties.JPAProperties;
import com.proit.app.repository.generic.CustomJpaRepositoryFactoryBean;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@EnableTransactionManagement
@ConditionalOnProperty(value = JPAProperties.ACTIVATION_PROPERTY, matchIfMissing = true)
@EnableConfigurationProperties({JpaProperties.class, JPAProperties.class})
@PropertySource("classpath:jpa.properties")
@EnableJpaRepositories(value = {"com.proit"}, repositoryFactoryBeanClass = CustomJpaRepositoryFactoryBean.class)
public class JpaConfiguration
{
	// Убеждаемся, что при активированном DDL, он инициализируется до JPA.
	private final Optional<DDLConfiguration> ddlConfiguration;

	private final List<EntityManagerFactoryCustomizer> entityManagerFactoryCustomizers;

	private final JpaProperties jpaProperties;

	@Value("${app.module:app}")
	private String appModule;

	@Bean
	public JpaVendorAdapter jpaVendorAdapter()
	{
		return new EclipseLinkJpaVendorAdapter();
	}

	@Bean
	public Customizer jpaCustomizer()
	{
		return new Customizer();
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource, Customizer customizer)
	{
		LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();

		emf.setDataSource(dataSource);
		emf.setJpaVendorAdapter(jpaVendorAdapter());
		emf.getJpaPropertyMap().putAll(jpaProperties.getProperties());
		emf.setPersistenceUnitName(appModule);
		emf.setPersistenceUnitPostProcessors(entityManagerFactoryCustomizers.toArray(EntityManagerFactoryCustomizer[]::new));
		emf.setPackagesToScan(entityManagerFactoryCustomizers.stream().flatMap(it -> it.getPackagesToScan().stream()).toArray(String[]::new));
		emf.setMappingResources(entityManagerFactoryCustomizers.stream().flatMap(it -> it.getMappingResources().stream()).toArray(String[]::new));

		return emf;
	}

	@Bean
	public JpaTransactionManager jpaTransactionManager(EntityManagerFactory entityManagerFactory)
	{
		JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
		jpaTransactionManager.setEntityManagerFactory(entityManagerFactory);

		return jpaTransactionManager;
	}

	@Bean
	@ConditionalOnProperty(value = JPAProperties.META_MODEL_VALIDATION_ACTIVATION_PROPERTY, matchIfMissing = true)
	public MetaModelVerifier metaModelVerifier()
	{
		return new MetaModelVerifier();
	}
}
