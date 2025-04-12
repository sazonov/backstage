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
import com.proit.app.service.health.SimpleHealthIndicatorComponent;
import com.proit.app.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Health indicator который позволяет отслеживать:
 * <ul>
 *     <li>созданные на текущий момент партиции;</li>
 *     <li>таблицы, партциии которых пропущены;</li>
 *     <li>проблемы в конфигурации pg_partman</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = PartmanProperties.ACTIVATION_PROPERTY)
public class PartitionConfigHealthIndicator extends SimpleHealthIndicatorComponent
{
	private static final String MAX_LOWER_BOUND_OF_PARTS_QUERY = """
			select inh.inhparent::regclass::varchar parent,
			       max(lower_bound)                 lower
			from pg_inherits inh
			         join pg_class cls on cls.oid = inh.inhrelid,
			     pg_get_expr(cls.relpartbound, inh.inhrelid) partition_bound,
			     split_part(partition_bound, '''', 2) lower_bound,
			     split_part(partition_bound, '''', 4) upper_bound
			where inh.inhparent in (select parent_table::regclass from part_config)
			  and (partition_bound ilike '%from%' or partition_bound ilike 'default')
			group by parent;
			""";

	private static final String CHECK_PART_CONFIG_QUERY = """
			select parent_table as name
			from part_config
			where infinite_time_partitions = false;
			""";

	public static final RowMapper<Map.Entry<String, LocalDate>> MAX_BOUND_MAPPER = (rs, rowNum) ->
			Map.entry(
					rs.getString("parent"),
					DateUtils.toLocalDateTime(rs.getTimestamp("lower")).toLocalDate()
			);

	private final NamedParameterJdbcTemplate jdbc;

	@Override
	protected void doHealthCheck(Health.Builder builder)
	{
		var quarter = LocalDate.now().with(IsoFields.DAY_OF_QUARTER, 1L);

		var maxLowerBound = jdbc.query(MAX_LOWER_BOUND_OF_PARTS_QUERY, MAX_BOUND_MAPPER)
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		var disabledInfinityTimeTables = jdbc.queryForList(CHECK_PART_CONFIG_QUERY, Map.of(), String.class);

		var problemParents = maxLowerBound.entrySet()
				.stream()
				.filter(entry -> entry.getValue().isEqual(quarter) || entry.getValue().isBefore(quarter))
				.map(Map.Entry::getKey)
				.toList();

		var details = new LinkedHashMap<String, Object>();
		details.put("currentYear", quarter.getYear());
		details.put("currentQuarter", quarter.get(IsoFields.QUARTER_OF_YEAR));

		if (problemParents.isEmpty() || disabledInfinityTimeTables.isEmpty())
		{
			builder.withDetails(details)
					.up();
		}
		else
		{
			details.put("parentsWithPartitionProblem", problemParents);
			details.put("parentsWithConfigProblem", disabledInfinityTimeTables);

			builder.withDetails(details)
					.down();
		}
	}
}
