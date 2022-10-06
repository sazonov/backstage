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

package com.proit.app.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Set;

@Getter
@Setter
@Validated
@ConfigurationProperties("proit.business-calendar")
public class BusinessCalendarProperties
{
	public static final String ACTIVATING_PROPERTY = "proit.business-calendar.enabled";

	/**
	 * Активирует сервисы для работы с производственным календарём.
	 */
	private boolean enabled = false;

	/**
	 * Начало рабочего дня.
	 */
	private LocalTime dayStart = LocalTime.parse("10:00:00");

	/**
	 * Конец рабочего дня.
	 */
	private LocalTime dayEnd = LocalTime.parse("19:00:00");

	/**
	 * Рабочие дни недели.
	 */
	@NotEmpty
	private Set<DayOfWeek> workDays = Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

	/**
	 * Выходные дни, которые являются исключением из {@link #workDays}.
	 */
	private Set<LocalDate> extraWorkDays = Collections.emptySet();

	/**
	 * Праздники.
	 */
	private Set<LocalDate> vacations = Collections.emptySet();
}
