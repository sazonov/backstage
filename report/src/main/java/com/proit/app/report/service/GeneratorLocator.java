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

package com.proit.app.report.service;

import com.proit.app.report.exception.ReportGeneratorNotFoundException;
import com.proit.app.report.model.ReportType;
import com.proit.app.report.model.filter.ReportFilter;
import com.proit.app.report.service.generator.ReportGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeneratorLocator
{
	/**
	 * Все имплементации {@link ReportGenerator}
	 */
	private final List<ReportGenerator<? extends ReportFilter>> generators;

	private final Map<Class<?>, ReportGenerator<? extends ReportFilter>> generatorByReportTypeName = new HashMap<>();

	@PostConstruct
	void init()
	{
		log.info("Найдено имплементаций ReportGenerator: {}.", generators.size());

		generators.forEach(it -> generatorByReportTypeName.put(it.getClass(), it));
	}

	@SuppressWarnings("unchecked")
	public <T extends ReportFilter> ReportGenerator<T> getGenerator(ReportType reportType)
	{
		return Optional.ofNullable(reportType)
				.map(ReportType::getGeneratorType)
				// TODO: 25.10.2023 Рассмотреть вариант отказа от локатора в пользу извлечения напрямую из контекста
				.map(clazz -> (ReportGenerator<T>) generatorByReportTypeName.get(clazz))
				.orElseThrow(() -> new ReportGeneratorNotFoundException("Не найден генератор для типа отчета %s.".formatted(reportType)));
	}
}
