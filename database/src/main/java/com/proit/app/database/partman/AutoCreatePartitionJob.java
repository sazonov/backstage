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

package com.proit.app.database.partman;

import com.proit.app.database.configuration.properties.PartmanProperties;
import com.proit.app.jobs.model.dto.other.JobResult;
import com.proit.app.jobs.model.dto.param.EmptyJobParams;
import com.proit.app.jobs.service.AbstractCronJob;
import com.proit.app.jobs.service.JobDescription;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

/**
 * Джоба по автоматическому созданию новых партиций для расширения <i>pg_partman</i>
 * <br/>
 * Вызов процедуры вынесен на уровень кода из-за неудобства в использовании расширения <i>pg_cron</i>.
 * <p>
 * Для создания партиций по вызову процедуры <b>infinite_time_partitions</b> должна быть <b>true</b>.
 * <br/>
 * <a href="https://github.com/pgpartman/pg_partman/blob/master/doc/pg_partman.md">Ссылка на документацию</a>
 */
@Component
@ConditionalOnProperty(value = PartmanProperties.ACTIVATION_PROPERTY)
@JobDescription("Автоматическое создание партиций через вызов процедуры в БД")
public class AutoCreatePartitionJob extends AbstractCronJob<EmptyJobParams>
{
	private static final String MAINTENANCE_PROC_CALL = "call run_maintenance_proc()";

	private final PartmanProperties properties;

	private final JdbcTemplate jdbc;

	public AutoCreatePartitionJob(@Qualifier("ddlDataSource") DataSource dataSource,
	                              PartmanProperties properties)
	{
		super(properties.getCron());

		this.jdbc = new JdbcTemplate(dataSource);
		this.properties = properties;
	}

	@Override
	public String getCronExpression()
	{
		return properties.getCron();
	}

	@Override
	public JobResult execute()
	{
		jdbc.call(con -> con.prepareCall(MAINTENANCE_PROC_CALL), List.of());

		return JobResult.ok();
	}
}
