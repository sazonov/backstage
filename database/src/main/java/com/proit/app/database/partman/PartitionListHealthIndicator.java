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
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = PartmanProperties.ACTIVATION_PROPERTY)
public class PartitionListHealthIndicator extends SimpleHealthIndicatorComponent
{
	private static final String PARTS_OF_PARTITIONED_TABLES_QUERY = """
			select inhparent::regclass::varchar as parent, array_agg(inhrelid::regclass::varchar) as children
			from pg_catalog.pg_inherits
			where inhparent in (select parent_table::regclass from part_config)
			group by parent;
			""";

	public static final RowMapper<Map.Entry<String, List<String>>> ROW_MAPPER = (rs, rowNum) ->
			Map.entry(
					rs.getString("parent"),
					List.of((String[]) rs.getArray("children").getArray())
			);

	private final NamedParameterJdbcTemplate jdbc;

	@Override
	protected void doHealthCheck(Health.Builder builder)
	{
		var parts = jdbc.query(PARTS_OF_PARTITIONED_TABLES_QUERY, ROW_MAPPER)
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		if (!parts.isEmpty())
		{
			builder.withDetails(Map.of("parts", parts))
					.up();
		}
		else
		{
			builder.withDetails(Map.of("parts", List.of()))
					.down();
		}
	}
}
