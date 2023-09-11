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

package com.proit.app.configuration;

import com.proit.app.configuration.conditional.ConditionalOnAudit;
import com.proit.app.configuration.conditional.ConditionalOnJms;
import com.proit.app.configuration.conditional.ConditionalOnJpa;
import com.proit.app.configuration.properties.AuditProperties;
import com.proit.app.repository.AuditRepository;
import com.proit.app.service.audit.AuditStore;
import com.proit.app.service.audit.JmsAuditStore;
import com.proit.app.service.audit.JpaAuditStore;
import com.proit.app.service.audit.LogAuditStore;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jms.core.JmsTemplate;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuditProperties.class)
@ConditionalOnAudit
@RequiredArgsConstructor
public class AuditConfiguration
{
	@Bean
	@Primary
	@ConditionalOnJms
	public AuditStore jmsAuditWriter(NamedParameterJdbcTemplate jdbcTemplate, AuditRepository auditRepository, JmsTemplate jmsTemplate)
	{
		return new JmsAuditStore(jmsTemplate, jpaAuditWriter(jdbcTemplate, auditRepository));
	}

	@Bean
	@ConditionalOnJpa
	public AuditStore jpaAuditWriter(NamedParameterJdbcTemplate jdbcTemplate, AuditRepository auditRepository)
	{
		return new JpaAuditStore(jdbcTemplate, auditRepository);
	}

	@Bean
	@ConditionalOnMissingBean
	public AuditStore logAuditStore()
	{
		return new LogAuditStore();
	}
}
